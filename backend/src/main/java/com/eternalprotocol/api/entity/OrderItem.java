package com.eternalprotocol.api.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

/**
 * Database entity for one product line within an {@link Order} — a
 * specific product, size, colour, and quantity.
 * <p>
 * Maps onto the {@code order_items} table. An order has many of these,
 * which is what lets a single checkout (and single PayHere payment) cover
 * several different products and quantities greater than one.
 * <p>
 * <b>Composition:</b> the {@code order} field below is the "child" side of
 * the whole-part relationship declared on {@link Order#getItems()} — see
 * that class's JavaDoc for the {@code cascade}/{@code orphanRemoval} side
 * of the same relationship.
 */
@Entity
@Table(name = "order_items")
public class OrderItem {

    /** Auto-generated primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The order this line belongs to. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    /** The product variant purchased. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /** Size purchased, e.g. "M". */
    @Column(nullable = false)
    private String size;

    /** Colour purchased, e.g. "Black". */
    @Column(nullable = false)
    private String colour;

    /** Number of units purchased. */
    @Column(nullable = false)
    private Integer quantity;

    /**
     * The product's price at the moment this order was placed, copied from
     * {@link Product#getPrice()}. Stored separately (not looked up live)
     * so that if an admin changes the product's price later, past orders
     * still show what the customer actually paid at the time.
     */
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    /** Default constructor, required by JPA. */
    public OrderItem() {
    }

    /**
     * Creates a new order line.
     *
     * @param order     the order this line belongs to
     * @param product   product variant purchased
     * @param size      size purchased
     * @param colour    colour purchased
     * @param quantity  number of units
     * @param unitPrice price per unit at time of purchase
     */
    public OrderItem(Order order, Product product, String size, String colour, Integer quantity, BigDecimal unitPrice) {
        this.order = order;
        this.product = product;
        this.size = size;
        this.colour = colour;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getColour() {
        return colour;
    }

    public void setColour(String colour) {
        this.colour = colour;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    /**
     * Total price for this line, calculated on the fly as
     * {@code unitPrice × quantity}. Marked {@code @Transient} so it is
     * never stored as its own database column — it is always derived, so
     * it can never fall out of sync with the values it's calculated from.
     *
     * @return the line total
     */
    @Transient
    public BigDecimal getLineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
