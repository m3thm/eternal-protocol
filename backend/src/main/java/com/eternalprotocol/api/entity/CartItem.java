package com.eternalprotocol.api.entity;

import jakarta.persistence.*;

/**
 * Database entity for one line of a logged-in customer's saved cart.
 * <p>
 * Only exists for customers with an account, so their cart is remembered
 * between visits and devices. Guests keep their cart entirely in the
 * browser — nothing is written here until a logged-in customer adds an
 * item.
 */
@Entity
@Table(name = "cart_items")
public class CartItem {

    /** Auto-generated primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The customer this cart line belongs to. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    /** The product variant (specific colour/size row) added to the cart. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /** Size chosen, e.g. "M". */
    @Column(nullable = false)
    private String size;

    /** Colour chosen, e.g. "Black". */
    @Column(nullable = false)
    private String colour;

    /** How many units of this item are in the cart. */
    @Column(nullable = false)
    private Integer quantity = 1;

    /** Default constructor, required by JPA. */
    public CartItem() {
    }

    /**
     * Creates a new cart line for a customer.
     *
     * @param customer the owning customer
     * @param product  the product variant being added
     * @param size     size chosen
     * @param colour   colour chosen
     * @param quantity how many units
     */
    public CartItem(Customer customer, Product product, String size, String colour, Integer quantity) {
        this.customer = customer;
        this.product = product;
        this.size = size;
        this.colour = colour;
        this.quantity = quantity;
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
}
