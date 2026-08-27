package com.eternalprotocol.api.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
     * Free-text subcategory set by the admin (e.g. "T-Shirts", "Hoodies").
     * Unlike {@code category}, this isn't a fixed set — the storefront's
     * dropdown filters are populated from whatever values currently exist.
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

    /**
     * Where this variant sits in its storefront lifecycle — see
     * {@link ProductStatus}. Kept independent of {@link #stockQty}: this
     * is never inferred, only ever set explicitly by an admin.
     * <p>
     * {@code columnDefinition} gives existing rows a safe default when
     * this column is first added to an already-populated table.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "varchar(20) default 'ACTIVE'")
    private ProductStatus status = ProductStatus.ACTIVE;

    /**
     * Whether the price should be displayed to shoppers. Only meaningful
     * while {@link #status} is {@link ProductStatus#COMING_SOON} — an
     * {@code ACTIVE} product's price is always shown regardless of this
     * flag. Lets an admin choose whether to reveal pricing ahead of
     * launch.
     */
    @Column(name = "show_price", nullable = false, columnDefinition = "boolean default true")
    private boolean showPrice = true;

    /**
     * When this product variant was first created. Set automatically on
     * insert (see {@link #onCreate}) and never changed afterward — not
     * admin-editable, and untouched by updates. Powers the New Drops page
     * and the homepage's "New drops" rail.
     */
    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "timestamp default current_timestamp")
    private LocalDateTime createdAt;

    /**
     * JPA lifecycle callback that stamps {@link #createdAt} the moment
     * this row is first persisted. Deliberately not settable through
     * {@link com.eternalprotocol.api.dto.ProductRequestDto} — the admin
     * doesn't choose this value, the database does.
     */
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

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

    public ProductStatus getStatus() {
        return status;
    }

    public void setStatus(ProductStatus status) {
        this.status = status;
    }

    public boolean isShowPrice() {
        return showPrice;
    }

    public void setShowPrice(boolean showPrice) {
        this.showPrice = showPrice;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
