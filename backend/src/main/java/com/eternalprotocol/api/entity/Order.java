package com.eternalprotocol.api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A single checkout — one customer, one payment, one or more OrderItems.
 * Deliberately does NOT hold product/size/colour/quantity directly (that
 * was the pre-multi-item design); every line of what was actually bought
 * lives in `items`. discountAmount/totalAmount are order-level because an
 * athlete discount code applies to the whole purchase, not per line.
 */
@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderItem> items = new ArrayList<>();

    // Sum of every item's (unitPrice x quantity), before discount.
    @Column(name = "subtotal_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotalAmount;

    @Column(name = "discount_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    // subtotalAmount - discountAmount: what the customer actually pays online.
    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private OrderStatus paymentStatus = OrderStatus.PENDING;

    @Column(name = "athlete_code")
    private String athleteCode; // stored on the order even if athlete is later deactivated

    @Column(name = "order_date", nullable = false)
    private LocalDateTime orderDate = LocalDateTime.now();

    @Column(name = "payhere_order_id", unique = true)
    private String payhereOrderId; // our own order reference sent to PayHere as `order_id`

    public Order(Customer customer, BigDecimal subtotalAmount, BigDecimal discountAmount, String athleteCode) {
        this.customer = customer;
        this.subtotalAmount = subtotalAmount;
        this.discountAmount = discountAmount;
        this.totalAmount = subtotalAmount.subtract(discountAmount);
        this.athleteCode = athleteCode;
        this.paymentStatus = OrderStatus.PENDING;
        this.orderDate = LocalDateTime.now();
    }

    /** Adds an item and sets both sides of the bidirectional link — always use this, never `items.add(...)` directly. */
    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }
}
