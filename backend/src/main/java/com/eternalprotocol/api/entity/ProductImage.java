package com.eternalprotocol.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Database entity for a single gallery image belonging to one colour of
 * one product style.
 * <p>
 * Maps onto the {@code product_images} table. Deliberately not linked to a
 * specific {@link Product} row — a style/colour combination (e.g.
 * "PRT-001" / "Black") can have several Product rows, one per size, and
 * all of them share the same photos. Images are instead keyed on
 * (styleCode, colour), which lets the product page swap its whole image
 * set the moment a shopper picks a different colour, without duplicating
 * the same image for every size.
 */
@Entity
@Table(name = "product_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
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
     * The image's ID on Cloudinary (Cloudinary's own "public_id"), kept
     * alongside the URL so a delete can remove the exact asset from
     * Cloudinary directly, instead of trying to work out an asset ID from
     * the URL text. Optional only so images created before this field
     * existed don't break — every new image always has one.
     */
    @Column(name = "cloudinary_public_id")
    private String cloudinaryPublicId;

    /**
     * Whether this is the one "cover" image for its (styleCode, colour)
     * pair — the image shown on the shop grid and as the first photo on
     * the product page. Exactly one image per colour should have this set
     * to true. Automatically true for the first image uploaded to a new
     * colour, and can be changed by the admin afterward.
     */
    @Column(name = "is_cover", nullable = false)
    private boolean cover = false;

    /** Display order of this image within its colour's photo gallery, smallest shown first. */
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;
}
