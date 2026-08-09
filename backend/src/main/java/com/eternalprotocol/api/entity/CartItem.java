package com.eternalprotocol.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Server-persisted cart line, only used for logged-in customers so their
 * cart survives across devices. Guests and not-yet-logged-in users keep
 * their cart entirely client-side (see frontend CartContext) — nothing here
 * is created until a customer with an account adds an item while logged in.
 */
@Entity
@Table(name = "cart_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private String size;

    @Column(nullable = false)
    private String colour;

    @Column(nullable = false)
    private Integer quantity = 1;

    public CartItem(Customer customer, Product product, String size, String colour, Integer quantity) {
        this.customer = customer;
        this.product = product;
        this.size = size;
        this.colour = colour;
        this.quantity = quantity;
    }
}
