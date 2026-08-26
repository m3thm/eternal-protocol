package com.eternalprotocol.api.service;

import com.eternalprotocol.api.dto.*;
import com.eternalprotocol.api.entity.*;
import com.eternalprotocol.api.exception.InsufficientStockException;
import com.eternalprotocol.api.exception.ResourceNotFoundException;
import com.eternalprotocol.api.factory.CustomerFactory;
import com.eternalprotocol.api.referral.AppliedAthleteReferral;
import com.eternalprotocol.api.referral.AthleteReferral;
import com.eternalprotocol.api.referral.NoAthleteReferral;
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

/**
 * Business logic for placing orders, confirming PayHere payments, and
 * reading back order history.
 * <p>
 * Most read methods here are {@code @Transactional(readOnly = true)}. This
 * matters because every DTO built here reads {@code order.getItems()} and
 * each item's related product/customer — all "lazy" associations that are
 * only fetched from the database when actually read. With
 * {@code open-in-view: false} in this app's configuration, those lazy
 * reads only work while the original database session is still open.
 * Building the DTO inside the same {@code @Transactional} method (instead
 * of returning a raw entity for something else to convert later) is what
 * keeps that session open long enough. Skipping the annotation won't fail
 * in tests, since the test framework happens to wrap a whole test method
 * in one shared transaction — but it will throw a
 * {@code LazyInitializationException} against a real frontend request,
 * where no such wrapping transaction exists.
 */
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CustomerFactory customerFactory;
    private final AthleteRepository athleteRepository;
    private final CommissionRepository commissionRepository;
    private final CartService cartService;
    private final PaymentService paymentService;

    /**
     * @param orderRepository      access to order data
     * @param productRepository    access to product data, for stock checks and pricing
     * @param customerFactory      resolves which customer a new order is attached to (Simple Factory — see that class)
     * @param athleteRepository    access to athlete data, for discount code lookup
     * @param commissionRepository access to commission data
     * @param cartService          clears a customer's saved cart after checkout
     * @param paymentService       builds the PayHere payment flow
     */
    public OrderService(OrderRepository orderRepository,
                         ProductRepository productRepository,
                         CustomerFactory customerFactory,
                         AthleteRepository athleteRepository,
                         CommissionRepository commissionRepository,
                         CartService cartService,
                         PaymentService paymentService) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.customerFactory = customerFactory;
        this.athleteRepository = athleteRepository;
        this.commissionRepository = commissionRepository;
        this.cartService = cartService;
        this.paymentService = paymentService;
    }

    /**
     * Places a new order: validates stock, applies any athlete discount,
     * saves the order and its items, reserves stock, creates a commission
     * if applicable, and kicks off the PayHere payment flow.
     * <p>
     * One call to this method is always one order and one PayHere payment,
     * no matter how many different products or quantities are in
     * {@code request.items()} — this is what lets an entire cart check out
     * as a single purchase, instead of one payment per product line.
     * <p>
     * Works identically for guest checkout and logged-in customers.
     * {@code loggedInCustomerId} is null for guests, in which case a fresh
     * {@code Customer} row is created just for this order, exactly as it
     * worked before customer accounts existed.
     *
     * @param request             checkout details: items, delivery info, optional athlete code
     * @param loggedInCustomerId  the current logged-in customer's ID, or null for a guest checkout
     * @return the created order's ID, total, and PayHere payment URL
     * @throws ResourceNotFoundException   if a requested product or the logged-in customer doesn't exist
     * @throws InsufficientStockException  if any requested item exceeds available stock
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

        // Athlete code, if any, applies once to the whole order — not per
        // line. Resolved as an AthleteReferral (Null Object pattern — see
        // that interface) rather than a nullable Athlete, so nothing below
        // needs an "if there was an athlete" check: an invalid/inactive
        // code simply resolves to NoAthleteReferral, which behaves exactly
        // like "no discount, no commission" without special-casing it.
        AthleteReferral referral = resolveReferral(request.athleteCode());

        // Resolve the customer via CustomerFactory (Simple Factory): reuse
        // the logged-in account if present, otherwise create a fresh
        // guest row — see that class for why this decision-and-construction
        // logic lives in its own class rather than inline here.
        Customer customer = customerFactory.resolveForCheckout(loggedInCustomerId, request);

        BigDecimal subtotal = request.items().stream()
                .map(item -> productsById.get(item.productId()).getPrice()
                        .multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discount = referral.calculateDiscount(subtotal);

        Order order = new Order(customer, subtotal, discount, referral.athleteCodeForOrder());

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

        // Unconditional — see AthleteReferral: NoAthleteReferral's
        // implementation of this is a no-op, so this line doesn't need an
        // "if there was an athlete" check the way it used to.
        referral.recordCommission(commissionRepository, order, discount);

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
     * Marks an order as paid after PayHere confirms payment. Only ever
     * called from the PayHere webhook, and only after the caller has
     * already verified the webhook's signature — this method assumes that
     * check has already happened and does not repeat it.
     * <p>
     * Safe to call more than once for the same order (idempotent), since
     * PayHere's webhook can fire more than once for the same payment.
     *
     * @param orderId order to mark as paid
     * @throws ResourceNotFoundException if no order exists with this ID
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

    /**
     * Gets full details of a single order, used for the order confirmation
     * page.
     *
     * @param orderId order's database ID
     * @return the order's full details
     * @throws ResourceNotFoundException if no order exists with this ID
     */
    @Transactional(readOnly = true)
    public OrderDetailDto getOrderDetail(Long orderId) {
        return toDetailDto(orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId)));
    }

    /**
     * Gets a customer's order history, most recent first.
     *
     * @param customerId customer's database ID
     * @return that customer's orders, newest first
     */
    @Transactional(readOnly = true)
    public List<OrderDetailDto> getOrdersForCustomer(Long customerId) {
        return orderRepository.findByCustomerIdOrderByOrderDateDesc(customerId).stream()
                .map(this::toDetailDto)
                .toList();
    }

    /**
     * Builds the self-submitting PayHere checkout HTML for an order. Kept
     * as a single {@code @Transactional} method (rather than fetching the
     * {@code Order} entity here and handing it to {@code PaymentService}
     * afterward) for the same lazy-loading reason described on the class:
     * {@code order.getItems()} and {@code order.getCustomer()} must be
     * read while the database session is still open.
     *
     * @param orderId order to build the payment page for
     * @return the complete HTML page
     * @throws ResourceNotFoundException if no order exists with this ID
     */
    @Transactional(readOnly = true)
    public String buildCheckoutRedirectHtml(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
        return paymentService.buildCheckoutFormHtml(order);
    }

    /**
     * Gets every order in the system, most recent first, for the admin
     * dashboard, optionally filtered by payment status.
     *
     * @param statusFilter payment status name to filter by (e.g. "PAID"), or null/blank for all
     * @return matching orders
     */
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

    /**
     * Resolves whatever athlete referral applies to a checkout, always
     * returning a real, non-null {@link AthleteReferral} — never a
     * nullable {@code Athlete} for callers to check themselves.
     * <p>
     * A blank/missing code, or one that doesn't match any active athlete,
     * both resolve to the same {@link NoAthleteReferral#INSTANCE} — from
     * {@code createOrder}'s point of view "no code entered" and "invalid
     * code entered" are the same case, and checkout must not fail either
     * way (see the class-level note on {@code AthleteReferral}).
     *
     * @param athleteCode the code submitted at checkout, may be null/blank
     * @return the resolved referral, real or the Null Object
     */
    private AthleteReferral resolveReferral(String athleteCode) {
        if (athleteCode == null || athleteCode.isBlank()) {
            return NoAthleteReferral.INSTANCE;
        }
        return athleteRepository.findByAthleteCodeIgnoreCase(athleteCode)
                .filter(Athlete::isActive)
                .<AthleteReferral>map(AppliedAthleteReferral::new)
                .orElse(NoAthleteReferral.INSTANCE);
    }

    /**
     * Converts a database entity into its public-facing DTO shape.
     *
     * @param o the entity to convert
     * @return the equivalent DTO
     */
    private OrderDetailDto toDetailDto(Order o) {
        return new OrderDetailDto(
                o.getId(), toLineItemDtos(o), o.getSubtotalAmount(), o.getDiscountAmount(), o.getTotalAmount(),
                o.getPaymentStatus().name(), o.getAthleteCode(), o.getOrderDate());
    }

    /**
     * Converts a database entity into its public-facing DTO shape, for the
     * admin order list.
     *
     * @param o the entity to convert
     * @return the equivalent DTO
     */
    private AdminOrderDto toAdminDto(Order o) {
        return new AdminOrderDto(
                o.getId(), o.getCustomer().getName(), o.getCustomer().getPhone(), o.getCustomer().getAddress(),
                toLineItemDtos(o), o.getSubtotalAmount(), o.getDiscountAmount(), o.getTotalAmount(),
                o.getPaymentStatus().name(), o.getAthleteCode(), o.getOrderDate());
    }

    /**
     * Converts an order's items into their DTO shape. Package-private and
     * static so {@link AthleteService#getDashboard} can reuse this exact
     * same line-item mapping for the athlete dashboard, instead of
     * duplicating the mapping logic in two places.
     *
     * @param o the order whose items should be converted
     * @return the order's items as DTOs
     */
    static List<OrderLineItemDto> toLineItemDtos(Order o) {
        return o.getItems().stream()
                .map(i -> new OrderLineItemDto(
                        i.getProduct().getId(), i.getProduct().getName(), i.getSize(), i.getColour(),
                        i.getQuantity(), i.getUnitPrice(), i.getLineTotal()))
                .collect(Collectors.toList());
    }
}
