package com.eternalprotocol.api.entity;

import jakarta.persistence.*;

/**
 * Database entity for one line of a logged-in customer's saved cart.
 * <p>
 * Maps onto the {@code cart_items} table. This only exists for customers
 * with an account, so their cart is remembered between visits and devices.
 * Guests (not logged in) keep their cart entirely in the browser on the
 * frontend — nothing is written here until a logged-in customer adds an
 * item.
 * <p>
 * <b>Aggregation:</b> a Customer "has" CartItems, but a CartItem is only
 * ever read one at a time by ID here (there's no {@code List<CartItem>}
 * field on {@link Customer}), so the relationship is expressed from the
 * "many" side only, via the {@code customer} field below.
 */
@Entity
@Table(name = "cart_items")
public class CartItem {

    /** Auto-generated primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The customer this cart line belongs to.
     * {@code @ManyToOne} means many CartItem rows can point to the same
     * Customer. {@code FetchType.LAZY} means the related Customer row is
     * only loaded from the database when it's actually accessed in code,
     * not automatically every time a CartItem is loaded.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    /** The product variant (specific colour/size row) added to the cart. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /** Size chosen, e.g. "M". Stored separately since one Product row is one size/colour combo already, kept here for clarity of intent. */
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
