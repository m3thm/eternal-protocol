package com.eternalprotocol.api.repository;

import com.eternalprotocol.api.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    /**
     * The whole gallery for a style, every colour included — one call for
     * the PDP to fetch, then the frontend groups by colour client-side.
     * Ordered so each colour's images already come out in carousel order.
     */
    List<ProductImage> findByStyleCodeOrderByColourAscSortOrderAsc(String styleCode);

    /** Used when adding a new image, to work out the next sort position for that colour. */
    long countByStyleCodeAndColour(String styleCode, String colour);

    /** Used to keep Product.imageUrl (the shop-grid thumbnail) in sync after an add/delete/reorder. */
    List<ProductImage> findByStyleCodeAndColourOrderBySortOrderAsc(String styleCode, String colour);

    /** The one image marked as cover for a colour, if any — drives Product.imageUrl and the PDP's first frame. */
    Optional<ProductImage> findByStyleCodeAndColourAndCoverTrue(String styleCode, String colour);
}
