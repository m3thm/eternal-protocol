package com.eternalprotocol.api.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

/**
 * Database entity for a single purchasable variant (one specific colour +
 * size combination) of a clothing item.
 * <p>
 * Maps onto the {@code products} table. Multiple Product rows sharing the
 * same {@code styleCode} represent the same "style" shown as one card on
 * the Shop page — for example "Protocol 001 Oversized Tee" in Black/M,
 * Black/L, and White/M are three separate Product rows that all share one
 * styleCode. Using an explicit admin-set code (rather than guessing from
 * the name) avoids two different products with the same name accidentally
 * being grouped together.
 */
@Entity
@Table(name = "products")
public class Product {

    /** Auto-generated primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Admin-assigned code shared by every colour/size variant of the same style, e.g. "PRT-001". */
    @Column(name = "style_code", nullable = false)
    private String styleCode;

    /** Product name shown to customers. */
    @Column(nullable = false)
    private String name;

    /** One of the three fixed top-level storefront sections — see {@link ProductCategory}. */
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private ProductCategory category;

    /**
     * Free-text subcategory set by the admin (e.g. "T-Shirts", "Hoodies",
     * "Caps", "Bags"). Unlike {@code category}, this isn't a fixed set of
     * values — new subcategories can be added over time. The storefront's
     * dropdown filters are populated from whatever distinct values
     * currently exist for a category.
     */
    @Column(name = "sub_category", nullable = false)
    private String subCategory;

    /** Price of this variant. */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /** Colour of this specific variant, e.g. "Black". */
    @Column(nullable = false)
    private String colour;

    /** Size of this specific variant, e.g. "M". */
    @Column(nullable = false)
    private String size;

    /** Units currently in stock for this exact colour/size combination. */
    @Column(name = "stock_qty", nullable = false)
    private Integer stockQty;

    /** URL of the main image shown for this product on listing pages. */
    @Column(name = "image_url")
    private String imageUrl;

    /** Longer free-text description shown on the product detail page. */
    @Column(columnDefinition = "TEXT")
    private String description;

    /** Default constructor, required by JPA. */
    public Product() {
    }

    /**
     * Creates a fully-specified product variant.
     *
     * @param styleCode   shared code for every colour/size of this style
     * @param name        display name
     * @param category    top-level storefront category
     * @param subCategory free-text subcategory
     * @param price       price of this variant
     * @param colour      colour of this variant
     * @param size        size of this variant
     * @param stockQty    units currently in stock
     * @param imageUrl    main listing image URL
     * @param description longer product description
     */
    public Product(String styleCode, String name, ProductCategory category, String subCategory,
                    BigDecimal price, String colour, String size, Integer stockQty,
                    String imageUrl, String description) {
        this.styleCode = styleCode;
        this.name = name;
        this.category = category;
        this.subCategory = subCategory;
        this.price = price;
        this.colour = colour;
        this.size = size;
        this.stockQty = stockQty;
        this.imageUrl = imageUrl;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStyleCode() {
        return styleCode;
    }

    public void setStyleCode(String styleCode) {
        this.styleCode = styleCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ProductCategory getCategory() {
        return category;
    }

    public void setCategory(ProductCategory category) {
        this.category = category;
    }

    public String getSubCategory() {
        return subCategory;
    }

    public void setSubCategory(String subCategory) {
        this.subCategory = subCategory;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getColour() {
        return colour;
    }

    public void setColour(String colour) {
        this.colour = colour;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public Integer getStockQty() {
        return stockQty;
    }

    public void setStockQty(Integer stockQty) {
        this.stockQty = stockQty;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
