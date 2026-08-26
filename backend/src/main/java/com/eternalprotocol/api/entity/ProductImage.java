package com.eternalprotocol.api.entity;

import jakarta.persistence.*;

/**
 * Database entity for a single gallery image belonging to one colour of
 * one product style.
 * <p>
 * Deliberately not linked to a specific {@link Product} row — a
 * style/colour combination (e.g. "PRT-001" / "Black") can have several
 * Product rows, one per size, and all of them share the same photos.
 * Images are instead keyed on (styleCode, colour), so the product page can
 * swap its whole image set when a shopper picks a different colour,
 * without duplicating images per size.
 */
@Entity
@Table(name = "product_images")
public class ProductImage {

    /** Auto-generated primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Style this image belongs to — matches {@link Product#getStyleCode()}. */
    @Column(name = "style_code", nullable = false)
    private String styleCode;

    /** Colour this image belongs to, e.g. "Black". */
    @Column(nullable = false)
    private String colour;

    /** Public URL of the image, hosted on Cloudinary. */
    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    /**
     * The image's ID on Cloudinary, kept alongside the URL so a delete can
     * remove the exact asset directly. Optional only so images created
     * before this field existed don't break.
     */
    @Column(name = "cloudinary_public_id")
    private String cloudinaryPublicId;

    /**
     * Whether this is the "cover" image for its (styleCode, colour) pair —
     * shown on the shop grid and as the first product-page photo. Exactly
     * one image per colour should have this set to true.
     */
    @Column(name = "is_cover", nullable = false)
    private boolean cover = false;

    /** Display order of this image within its colour's photo gallery, smallest shown first. */
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    /** Default constructor, required by JPA. */
    public ProductImage() {
    }

    /**
     * Convenience constructor for creating a fully-specified gallery image
     * in one call.
     *
     * @param styleCode  style this image belongs to
     * @param colour     colour this image belongs to
     * @param imageUrl   public Cloudinary URL
     * @param cover      whether this is the colour's cover image
     * @param sortOrder  position within the gallery
     */
    public ProductImage(String styleCode, String colour, String imageUrl, boolean cover, Integer sortOrder) {
        this.styleCode = styleCode;
        this.colour = colour;
        this.imageUrl = imageUrl;
        this.cover = cover;
        this.sortOrder = sortOrder;
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

    public String getColour() {
        return colour;
    }

    public void setColour(String colour) {
        this.colour = colour;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getCloudinaryPublicId() {
        return cloudinaryPublicId;
    }

    public void setCloudinaryPublicId(String cloudinaryPublicId) {
        this.cloudinaryPublicId = cloudinaryPublicId;
    }

    public boolean isCover() {
        return cover;
    }

    public void setCover(boolean cover) {
        this.cover = cover;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
