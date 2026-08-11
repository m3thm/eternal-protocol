package com.eternalprotocol.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A single gallery image belonging to one colour of one style. Deliberately
 * NOT linked to a specific {@link Product} row: a style/colour combination
 * (e.g. "PRT-001" / "Black") can have several Product rows — one per size —
 * and all of them share the same photos, so images are keyed on
 * (styleCode, colour) rather than on an individual size row. This is what
 * lets the PDP carousel swap its whole image set the moment the shopper
 * picks a different colour, without duplicating the same image per size.
 */
@Entity
@Table(name = "product_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "style_code", nullable = false)
    private String styleCode;

    @Column(nullable = false)
    private String colour;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    /**
     * The Cloudinary asset ID returned at upload time (Cloudinary's
     * `public_id`), kept alongside the URL so a delete can call
     * `cloudinary.uploader().destroy(publicId, ...)` precisely instead of
     * trying to reverse-engineer an asset ID out of the URL string. Nullable
     * only so older rows created before this field existed don't break —
     * every image added going forward always has one.
     */
    @Column(name = "cloudinary_public_id")
    private String cloudinaryPublicId;

    /**
     * Exactly one image per (styleCode, colour) is the cover — the one
     * shown as Product.imageUrl on the shop grid and as the PDP carousel's
     * first frame. Set automatically to the first image uploaded for a new
     * colour, and explicitly by the admin afterward via "Set as cover".
     */
    @Column(name = "is_cover", nullable = false)
    private boolean cover = false;

    /** Carousel order within this colour, ascending. Set by the admin UI's reorder controls. */
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;
}
