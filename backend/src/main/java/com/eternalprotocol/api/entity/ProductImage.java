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

    /** Carousel order within this colour, ascending. Set by the admin UI's reorder controls. */
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;
}
