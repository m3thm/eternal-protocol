package com.eternalprotocol.api.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Database entity for a single checkout: one customer, one payment, and one
 * or more {@link OrderItem} lines.
 * <p>
 * Deliberately does not store product/size/colour/quantity directly —
 * every product bought is a separate row in {@code items}. Discount and
 * total live here (not per item) since an athlete discount applies to the
 * whole order, not individual lines.
 * <p>
 * An Order owns its OrderItems: {@code cascade = CascadeType.ALL} and
 * {@code orphanRemoval = true} below mean an item is always created
 * through {@link #addItem} and is deleted automatically once removed from
 * this list or the Order itself is deleted.
 */
@Entity
@Table(name = "orders")
public class Order {

    /** Auto-generated primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The customer who placed this order. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    /** All product lines in this order. */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderItem> items = new ArrayList<>();

    /** Sum of every item's (unit price × quantity), before any discount. */
    @Column(name = "subtotal_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotalAmount;

    /** Amount taken off the subtotal by an athlete discount code, if any. Zero if none was used. */
    @Column(name = "discount_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    /** What the customer actually pays: {@code subtotalAmount - discountAmount}. */
    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    /** Current payment state of this order — see {@link OrderStatus}. */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private OrderStatus paymentStatus = OrderStatus.PENDING;

    /** Athlete code used at checkout, if any. Kept on the order permanently, even if the athlete is later deactivated. */
    @Column(name = "athlete_code")
    private String athleteCode;

    /** Date and time the order was placed. */
    @Column(name = "order_date", nullable = false)
    private LocalDateTime orderDate = LocalDateTime.now();

    /** This app's own order reference, sent to PayHere as its {@code order_id} field. */
    @Column(name = "payhere_order_id", unique = true)
    private String payhereOrderId;

    /** Default constructor, required by JPA. */
    public Order() {
    }

    /**
     * Creates a new order with total automatically calculated as
     * {@code subtotalAmount - discountAmount}.
     *
     * @param customer        customer placing the order
     * @param subtotalAmount  sum of all item lines before discount
     * @param discountAmount  amount discounted, zero if no athlete code used
     * @param athleteCode     athlete code used at checkout, or null
     */
    public Order(Customer customer, BigDecimal subtotalAmount, BigDecimal discountAmount, String athleteCode) {
        this.customer = customer;
        this.subtotalAmount = subtotalAmount;
        this.discountAmount = discountAmount;
        this.totalAmount = subtotalAmount.subtract(discountAmount);
        this.athleteCode = athleteCode;
        this.paymentStatus = OrderStatus.PENDING;
        this.orderDate = LocalDateTime.now();
    }

    /**
     * Adds an item to this order and links it back to the order. Always use
     * this method instead of calling {@code items.add(...)} directly, so
     * both sides of the relationship stay in sync.
     *
     * @param item the order item to add
     */
    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }

    public BigDecimal getSubtotalAmount() {
        return subtotalAmount;
    }

    public void setSubtotalAmount(BigDecimal subtotalAmount) {
        this.subtotalAmount = subtotalAmount;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public OrderStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(OrderStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getAthleteCode() {
        return athleteCode;
    }

    public void setAthleteCode(String athleteCode) {
        this.athleteCode = athleteCode;
    }

    public LocalDateTime getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDateTime orderDate) {
        this.orderDate = orderDate;
    }

    public String getPayhereOrderId() {
        return payhereOrderId;
    }

    public void setPayhereOrderId(String payhereOrderId) {
        this.payhereOrderId = payhereOrderId;
    }
}
