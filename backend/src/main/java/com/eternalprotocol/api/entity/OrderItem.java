package com.eternalprotocol.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * One line within an Order — a specific product/size/colour and how many
 * of it. An Order has one-to-many of these, which is what actually lets a
 * single checkout (and single PayHere payment) cover multiple distinct
 * products and quantities > 1, instead of the old one-product-per-order
 * model.
 */
@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private String size;

    @Column(nullable = false)
    private String colour;

    @Column(nullable = false)
    private Integer quantity;

    // Snapshot of Product.price at the moment of order — same reasoning as
    // the old Order.productPrice field: if the admin changes the price
    // later, past orders must still reflect what the customer actually paid.
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    public OrderItem(Order order, Product product, String size, String colour, Integer quantity, BigDecimal unitPrice) {
        this.order = order;
        this.product = product;
        this.size = size;
        this.colour = colour;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    /** Not a persisted column — always derived, so it can never drift out of sync with quantity/unitPrice. */
    @Transient
    public BigDecimal getLineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
