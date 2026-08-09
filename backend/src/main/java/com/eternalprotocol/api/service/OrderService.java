package com.eternalprotocol.api.service;

import com.eternalprotocol.api.dto.*;
import com.eternalprotocol.api.entity.*;
import com.eternalprotocol.api.exception.InsufficientStockException;
import com.eternalprotocol.api.exception.ResourceNotFoundException;
import com.eternalprotocol.api.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final AthleteRepository athleteRepository;
    private final CommissionRepository commissionRepository;
    private final CartService cartService;
    private final PaymentService paymentService;

    public OrderService(OrderRepository orderRepository,
                         ProductRepository productRepository,
                         CustomerRepository customerRepository,
                         AthleteRepository athleteRepository,
                         CommissionRepository commissionRepository,
                         CartService cartService,
                         PaymentService paymentService) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.athleteRepository = athleteRepository;
        this.commissionRepository = commissionRepository;
        this.cartService = cartService;
        this.paymentService = paymentService;
    }

    /**
     * Creates a multi-item order + (optionally) a linked, not-yet-confirmed
     * commission, then kicks off the PayHere flow. One request = one order
     * = one PayHere payment, regardless of how many distinct products or
     * what quantities are in `request.items()` — this is what actually
     * lets a whole cart check out as a single purchase instead of one
     * PayHere payment per line.
     *
     * Works identically for guest checkout and logged-in customers —
     * loggedInCustomerId is null for guests, in which case a fresh
     * Customer row is created exactly as before customer accounts existed.
     */
    @Transactional
    public OrderResponseDto createOrder(OrderRequestDto request, Long loggedInCustomerId) {
        // Load every requested product up front and validate stock for
        // ALL items before creating or decrementing anything — an order
        // with 3 items where only the 3rd is out of stock should fail
        // cleanly with no partial order, not create 2 items then blow up.
        Map<Long, Product> productsById = new HashMap<>();
        List<String> insufficientStockMessages = new ArrayList<>();

        for (OrderItemRequestDto itemRequest : request.items()) {
            Product product = productRepository.findById(itemRequest.productId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + itemRequest.productId()));
            productsById.put(itemRequest.productId(), product);

            if (product.getStockQty() == null || product.getStockQty() < itemRequest.quantity()) {
                insufficientStockMessages.add("\"" + product.getName() + "\" (" + itemRequest.colour() + "/" + itemRequest.size() + ")"
                        + " — requested " + itemRequest.quantity()
                        + ", only " + (product.getStockQty() == null ? 0 : product.getStockQty()) + " left");
            }
        }

        if (!insufficientStockMessages.isEmpty()) {
            throw new InsufficientStockException(
                    "Not enough stock for: " + String.join("; ", insufficientStockMessages));
        }

        // Athlete code, if any, applies once to the whole order — not per line.
        Athlete athlete = null;
        if (request.athleteCode() != null && !request.athleteCode().isBlank()) {
            athlete = athleteRepository.findByAthleteCodeIgnoreCase(request.athleteCode())
                    .filter(Athlete::isActive)
                    .orElse(null); // invalid/inactive code -> silently no discount, don't fail checkout
        }

        // Resolve the customer: reuse the logged-in account if present,
        // otherwise create a fresh guest row (mirrors pre-login behaviour).
        Customer customer;
        if (loggedInCustomerId != null) {
            customer = customerRepository.findById(loggedInCustomerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
            // Keep delivery info current with whatever was submitted at checkout.
            customer.setName(request.customerName());
            customer.setPhone(request.phone());
            customer.setAddress(request.address());
            customer = customerRepository.save(customer);
        } else {
            customer = customerRepository.save(
                    new Customer(request.customerName(), request.phone(), request.address()));
        }

        BigDecimal subtotal = request.items().stream()
                .map(item -> productsById.get(item.productId()).getPrice()
                        .multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discount = athlete == null
                ? BigDecimal.ZERO
                : subtotal.multiply(athlete.getCommissionRate())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        Order order = new Order(customer, subtotal, discount, athlete != null ? athlete.getAthleteCode() : null);

        for (OrderItemRequestDto itemRequest : request.items()) {
            Product product = productsById.get(itemRequest.productId());
            order.addItem(new OrderItem(order, product, itemRequest.size(), itemRequest.colour(),
                    itemRequest.quantity(), product.getPrice()));
        }

        order = orderRepository.save(order);

        // Reserve stock at order-creation time (checkout submission), not
        // on payment confirmation — see class-level note on why, and the
        // known read-then-write race-condition tradeoff, unchanged from
        // the single-item version of this method.
        for (OrderItemRequestDto itemRequest : request.items()) {
            Product product = productsById.get(itemRequest.productId());
            product.setStockQty(product.getStockQty() - itemRequest.quantity());
            productRepository.save(product);
        }

        if (athlete != null) {
            commissionRepository.save(new Commission(athlete, order, discount));
        }

        // Logged-in customers checking out empties their persisted cart —
        // otherwise items they just bought would still show as "in cart"
        // the next time they load the page.
        if (loggedInCustomerId != null) {
            cartService.clearCart(loggedInCustomerId);
        }

        String paymentUrl = paymentService.initiatePayment(order, order.getTotalAmount());

        return new OrderResponseDto(order.getId(), order.getTotalAmount(), paymentUrl);
    }

    /**
     * Called by the PayHere webhook (notify_url). Only ever flips PENDING ->
     * PAID after the signature has already been verified by the caller —
     * this method assumes that check already happened.
     */
    @Transactional
    public void confirmPayment(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        if (order.getPaymentStatus() == OrderStatus.PAID) {
            return; // idempotent — webhook may fire more than once
        }

        order.setPaymentStatus(OrderStatus.PAID);
        orderRepository.save(order);

        commissionRepository.findByOrderId(order.getId()).ifPresent(commission -> {
            commission.setCommissionStatus(CommissionStatus.EARNED);
            commissionRepository.save(commission);
        });
    }

    // NOTE on @Transactional(readOnly = true) below: every one of these
    // methods maps an Order (or Commission -> Order) to a DTO that reads
    // order.getItems() and each item's order.getCustomer()/item.getProduct()
    // — both LAZY associations. With `open-in-view: false`, those are only
    // safely readable while the JPA session from the original query is
    // still open. Doing the DTO mapping inside the same @Transactional
    // method (rather than returning the raw entity for a caller to map
    // later) is what keeps that session alive long enough. Skipping this
    // annotation doesn't fail in tests — the test framework happens to wrap
    // a whole test method in one shared transaction — but WILL throw
    // LazyInitializationException against a real frontend, where no such
    // wrapping transaction exists.

    @Transactional(readOnly = true)
    public OrderDetailDto getOrderDetail(Long orderId) {
        return toDetailDto(orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId)));
    }

    @Transactional(readOnly = true)
    public List<OrderDetailDto> getOrdersForCustomer(Long customerId) {
        return orderRepository.findByCustomerIdOrderByOrderDateDesc(customerId).stream()
                .map(this::toDetailDto)
                .toList();
    }

    /**
     * Builds the PayHere auto-submit HTML for a given order — kept as one
     * transactional method (rather than returning the Order entity to the
     * controller for PaymentService to read afterward) for the same lazy-
     * loading reason as above: order.getItems() and order.getCustomer()
     * need to be read while the session is still open.
     */
    @Transactional(readOnly = true)
    public String buildCheckoutRedirectHtml(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
        return paymentService.buildCheckoutFormHtml(order);
    }

    // ---- Admin ----

    @Transactional(readOnly = true)
    public List<AdminOrderDto> getAllOrdersForAdmin(String statusFilter) {
        List<Order> orders;
        if (statusFilter != null && !statusFilter.isBlank()) {
            OrderStatus status = OrderStatus.valueOf(statusFilter.toUpperCase());
            orders = orderRepository.findByPaymentStatus(status);
        } else {
            orders = orderRepository.findAllByOrderByOrderDateDesc();
        }
        return orders.stream().map(this::toAdminDto).toList();
    }

    private OrderDetailDto toDetailDto(Order o) {
        return new OrderDetailDto(
                o.getId(), toLineItemDtos(o), o.getSubtotalAmount(), o.getDiscountAmount(), o.getTotalAmount(),
                o.getPaymentStatus().name(), o.getAthleteCode(), o.getOrderDate());
    }

    private AdminOrderDto toAdminDto(Order o) {
        return new AdminOrderDto(
                o.getId(), o.getCustomer().getName(), o.getCustomer().getPhone(), o.getCustomer().getAddress(),
                toLineItemDtos(o), o.getSubtotalAmount(), o.getDiscountAmount(), o.getTotalAmount(),
                o.getPaymentStatus().name(), o.getAthleteCode(), o.getOrderDate());
    }

    /** Shared by every DTO mapper above and by AthleteService's dashboard — one place builds the line-item list. */
    static List<OrderLineItemDto> toLineItemDtos(Order o) {
        return o.getItems().stream()
                .map(i -> new OrderLineItemDto(
                        i.getProduct().getId(), i.getProduct().getName(), i.getSize(), i.getColour(),
                        i.getQuantity(), i.getUnitPrice(), i.getLineTotal()))
                .collect(Collectors.toList());
    }
}
