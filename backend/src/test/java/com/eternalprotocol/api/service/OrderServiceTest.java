package com.eternalprotocol.api.service;

import com.eternalprotocol.api.dto.OrderItemRequestDto;
import com.eternalprotocol.api.dto.OrderRequestDto;
import com.eternalprotocol.api.dto.OrderResponseDto;
import com.eternalprotocol.api.entity.*;
import com.eternalprotocol.api.exception.InsufficientStockException;
import com.eternalprotocol.api.exception.ResourceNotFoundException;
import com.eternalprotocol.api.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private ProductRepository productRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private AthleteRepository athleteRepository;
    @Mock private CommissionRepository commissionRepository;
    @Mock private CartService cartService;
    @Mock private PaymentService paymentService;

    @InjectMocks
    private OrderService orderService;

    private Product tee;
    private Product hoodie;

    @BeforeEach
    void setUp() {
        tee = new Product();
        tee.setId(1L);
        tee.setStyleCode("PROTOCOL-001");
        tee.setName("Protocol 001 Oversized Tee");
        tee.setPrice(new BigDecimal("4500.00"));
        tee.setStockQty(10);

        hoodie = new Product();
        hoodie.setId(2L);
        hoodie.setStyleCode("PROTOCOL-002");
        hoodie.setName("Protocol 002 Hoodie");
        hoodie.setPrice(new BigDecimal("8000.00"));
        hoodie.setStockQty(5);
    }

    private void stubCommonSaves() {
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> {
            Customer c = inv.getArgument(0);
            c.setId(5L);
            return c;
        });
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(100L);
            return o;
        });
        when(paymentService.initiatePayment(any(Order.class), any(BigDecimal.class)))
                .thenReturn("http://localhost:8080/api/orders/100/checkout-redirect");
    }

    @Test
    void createOrder_singleItem_createsOneOrderWithOneOrderItem() {
        OrderItemRequestDto item = new OrderItemRequestDto(1L, "M", "Black", 1);
        OrderRequestDto request = new OrderRequestDto(
                "Jane Doe", "0771234567", "123 Main St", List.of(item), null);

        when(productRepository.findById(1L)).thenReturn(Optional.of(tee));
        stubCommonSaves();

        OrderResponseDto result = orderService.createOrder(request, null);

        assertThat(result.orderId()).isEqualTo(100L);
        assertThat(result.totalAmount()).isEqualByComparingTo("4500.00");

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getItems()).hasSize(1);
        assertThat(orderCaptor.getValue().getSubtotalAmount()).isEqualByComparingTo("4500.00");
    }

    @Test
    void createOrder_multipleDistinctProducts_createsOneOrderWithMultipleItemsAndOnePayment() {
        // The exact scenario from the bug report: 2 tees + 1 hoodie in one
        // cart must become ONE order and ONE PayHere payment, not three.
        OrderItemRequestDto teeItem = new OrderItemRequestDto(1L, "M", "Black", 2);
        OrderItemRequestDto hoodieItem = new OrderItemRequestDto(2L, "L", "Grey", 1);
        OrderRequestDto request = new OrderRequestDto(
                "Jane Doe", "0771234567", "123 Main St", List.of(teeItem, hoodieItem), null);

        when(productRepository.findById(1L)).thenReturn(Optional.of(tee));
        when(productRepository.findById(2L)).thenReturn(Optional.of(hoodie));
        stubCommonSaves();

        OrderResponseDto result = orderService.createOrder(request, null);

        // subtotal = (4500 x 2) + (8000 x 1) = 17000.00
        assertThat(result.totalAmount()).isEqualByComparingTo("17000.00");

        // Exactly ONE order was saved and ONE payment was initiated —
        // this is the property the bug report specifically flagged.
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(paymentService, times(1)).initiatePayment(any(Order.class), any(BigDecimal.class));

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getItems()).hasSize(2);
    }

    @Test
    void createOrder_quantityGreaterThanOne_reflectedInSubtotalAndSingleOrderItem() {
        OrderItemRequestDto item = new OrderItemRequestDto(1L, "M", "Black", 3);
        OrderRequestDto request = new OrderRequestDto(
                "Jane Doe", "0771234567", "123 Main St", List.of(item), null);

        when(productRepository.findById(1L)).thenReturn(Optional.of(tee));
        stubCommonSaves();

        OrderResponseDto result = orderService.createOrder(request, null);

        // 4500 x 3 = 13500.00, still ONE order/OrderItem, not three orders.
        assertThat(result.totalAmount()).isEqualByComparingTo("13500.00");
        verify(orderRepository, times(1)).save(any(Order.class));

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getItems()).hasSize(1);
        assertThat(orderCaptor.getValue().getItems().get(0).getQuantity()).isEqualTo(3);
    }

    @Test
    void createOrder_validAthleteCode_discountAppliesToWholeOrderSubtotal() {
        Athlete athlete = new Athlete();
        athlete.setId(7L);
        athlete.setAthleteCode("RISE10");
        athlete.setCommissionRate(new BigDecimal("10.00"));
        athlete.setActive(true);

        OrderItemRequestDto teeItem = new OrderItemRequestDto(1L, "M", "Black", 2);
        OrderItemRequestDto hoodieItem = new OrderItemRequestDto(2L, "L", "Grey", 1);
        OrderRequestDto request = new OrderRequestDto(
                "Jane Doe", "0771234567", "123 Main St", List.of(teeItem, hoodieItem), "RISE10");

        when(productRepository.findById(1L)).thenReturn(Optional.of(tee));
        when(productRepository.findById(2L)).thenReturn(Optional.of(hoodie));
        when(athleteRepository.findByAthleteCodeIgnoreCase("RISE10")).thenReturn(Optional.of(athlete));
        stubCommonSaves();

        OrderResponseDto result = orderService.createOrder(request, null);

        // subtotal 17000.00, 10% off the WHOLE order = 1700.00 discount -> 15300.00
        assertThat(result.totalAmount()).isEqualByComparingTo("15300.00");

        ArgumentCaptor<Commission> commissionCaptor = ArgumentCaptor.forClass(Commission.class);
        verify(commissionRepository).save(commissionCaptor.capture());
        assertThat(commissionCaptor.getValue().getCommissionAmount()).isEqualByComparingTo("1700.00");
    }

    @Test
    void createOrder_oneOfMultipleItemsOutOfStock_rejectsWholeOrderCreatesNothing() {
        hoodie.setStockQty(0); // the second item is unavailable
        OrderItemRequestDto teeItem = new OrderItemRequestDto(1L, "M", "Black", 1);
        OrderItemRequestDto hoodieItem = new OrderItemRequestDto(2L, "L", "Grey", 1);
        OrderRequestDto request = new OrderRequestDto(
                "Jane Doe", "0771234567", "123 Main St", List.of(teeItem, hoodieItem), null);

        when(productRepository.findById(1L)).thenReturn(Optional.of(tee));
        when(productRepository.findById(2L)).thenReturn(Optional.of(hoodie));

        assertThatThrownBy(() -> orderService.createOrder(request, null))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Protocol 002 Hoodie");

        // Nothing partially created — not even the customer row, and
        // definitely not an order for the one item that WAS in stock.
        verify(customerRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
        verify(productRepository, never()).save(any());
    }

    @Test
    void createOrder_insufficientQuantityForOneItem_throwsWithSpecificMessage() {
        OrderItemRequestDto item = new OrderItemRequestDto(1L, "M", "Black", 999); // way more than the 10 in stock
        OrderRequestDto request = new OrderRequestDto(
                "Jane Doe", "0771234567", "123 Main St", List.of(item), null);

        when(productRepository.findById(1L)).thenReturn(Optional.of(tee));

        assertThatThrownBy(() -> orderService.createOrder(request, null))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("requested 999")
                .hasMessageContaining("only 10 left");
    }

    @Test
    void createOrder_success_decrementsStockPerItemByItsOwnQuantity() {
        OrderItemRequestDto teeItem = new OrderItemRequestDto(1L, "M", "Black", 2);
        OrderItemRequestDto hoodieItem = new OrderItemRequestDto(2L, "L", "Grey", 1);
        OrderRequestDto request = new OrderRequestDto(
                "Jane Doe", "0771234567", "123 Main St", List.of(teeItem, hoodieItem), null);

        when(productRepository.findById(1L)).thenReturn(Optional.of(tee));
        when(productRepository.findById(2L)).thenReturn(Optional.of(hoodie));
        stubCommonSaves();

        orderService.createOrder(request, null);

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository, times(2)).save(productCaptor.capture());
        List<Product> saved = productCaptor.getAllValues();
        assertThat(saved).extracting(Product::getId, Product::getStockQty)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(1L, 8),  // 10 - 2
                        org.assertj.core.groups.Tuple.tuple(2L, 4)); // 5 - 1
    }

    @Test
    void createOrder_loggedInCustomer_clearsPersistedCartAfterSuccess() {
        Customer existing = new Customer("Jane", "0771234567", "123 Main St");
        existing.setId(5L);
        OrderItemRequestDto item = new OrderItemRequestDto(1L, "M", "Black", 1);
        OrderRequestDto request = new OrderRequestDto(
                "Jane Doe", "0771234567", "123 Main St", List.of(item), null);

        when(productRepository.findById(1L)).thenReturn(Optional.of(tee));
        when(customerRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(100L);
            return o;
        });
        when(paymentService.initiatePayment(any(Order.class), any(BigDecimal.class))).thenReturn("url");

        orderService.createOrder(request, 5L);

        verify(cartService).clearCart(5L);
    }

    @Test
    void createOrder_guestCheckout_neverTouchesCart() {
        OrderItemRequestDto item = new OrderItemRequestDto(1L, "M", "Black", 1);
        OrderRequestDto request = new OrderRequestDto(
                "Jane Doe", "0771234567", "123 Main St", List.of(item), null);

        when(productRepository.findById(1L)).thenReturn(Optional.of(tee));
        stubCommonSaves();

        orderService.createOrder(request, null);

        verify(cartService, never()).clearCart(any());
    }

    @Test
    void createOrder_unknownProduct_throwsResourceNotFoundException() {
        OrderItemRequestDto item = new OrderItemRequestDto(999L, "M", "Black", 1);
        OrderRequestDto request = new OrderRequestDto(
                "Jane Doe", "0771234567", "123 Main St", List.of(item), null);
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.createOrder(request, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void confirmPayment_pendingOrder_flipsToPaidAndCommissionToEarned() {
        Customer customer = new Customer("Jane Doe", "0771234567", "123 Main St");
        Order order = new Order(customer, new BigDecimal("4500.00"), BigDecimal.ZERO, "RISE10");
        order.setId(100L);
        order.setPaymentStatus(OrderStatus.PENDING);

        Athlete athlete = new Athlete();
        athlete.setId(7L);
        Commission commission = new Commission(athlete, order, new BigDecimal("450.00"));
        commission.setId(50L);

        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(commissionRepository.findByOrderId(100L)).thenReturn(Optional.of(commission));
        when(commissionRepository.save(any(Commission.class))).thenAnswer(inv -> inv.getArgument(0));

        orderService.confirmPayment(100L);

        assertThat(order.getPaymentStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(commission.getCommissionStatus()).isEqualTo(CommissionStatus.EARNED);
    }

    @Test
    void confirmPayment_alreadyPaid_isIdempotentAndDoesNotResave() {
        Customer customer = new Customer("Jane Doe", "0771234567", "123 Main St");
        Order order = new Order(customer, new BigDecimal("4500.00"), BigDecimal.ZERO, null);
        order.setId(100L);
        order.setPaymentStatus(OrderStatus.PAID);

        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        orderService.confirmPayment(100L);

        verify(orderRepository, never()).save(any());
        verify(commissionRepository, never()).findByOrderId(any());
    }

    @Test
    void confirmPayment_noLinkedCommission_stillMarksOrderPaid() {
        Customer customer = new Customer("Jane Doe", "0771234567", "123 Main St");
        Order order = new Order(customer, new BigDecimal("4500.00"), BigDecimal.ZERO, null);
        order.setId(100L);
        order.setPaymentStatus(OrderStatus.PENDING);

        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(commissionRepository.findByOrderId(100L)).thenReturn(Optional.empty());

        orderService.confirmPayment(100L);

        assertThat(order.getPaymentStatus()).isEqualTo(OrderStatus.PAID);
        verify(commissionRepository, never()).save(any());
    }
}
