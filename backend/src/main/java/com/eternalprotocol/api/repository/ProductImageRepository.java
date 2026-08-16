package com.eternalprotocol.api.repository;

import com.eternalprotocol.api.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Data access for {@link ProductImage} rows. Spring generates the
 * implementation of every method below automatically from its name.
 */
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    /**
     * Gets the entire photo gallery for a style, every colour included, in
     * one call. Results are ordered by colour then by sort order, so each
     * colour's photos already arrive in the correct carousel order — the
     * frontend just needs to group them by colour.
     *
     * @param styleCode style to fetch images for
     * @return all images for that style, grouped-ready and in display order
     */
    List<ProductImage> findByStyleCodeOrderByColourAscSortOrderAsc(String styleCode);

    /**
     * Counts how many images already exist for a style/colour pair, used
     * to work out the next sort position when adding a new image.
     *
     * @param styleCode style to check
     * @param colour    colour to check
     * @return number of existing images for that style/colour
     */
    long countByStyleCodeAndColour(String styleCode, String colour);

    /**
     * Gets all images for one style/colour pair in display order, used to
     * keep {@code Product.imageUrl} (the shop-grid thumbnail) in sync after
     * an image is added, deleted, or reordered.
     *
     * @param styleCode style to fetch
     * @param colour    colour to fetch
     * @return images for that style/colour, in display order
     */
    List<ProductImage> findByStyleCodeAndColourOrderBySortOrderAsc(String styleCode, String colour);

    /**
     * Finds the single image marked as the cover photo for a style/colour
     * pair, if one exists. Drives {@code Product.imageUrl} and the first
     * photo shown on the product detail page.
     *
     * @param styleCode style to check
     * @param colour    colour to check
     * @return the cover image, or empty if none is marked yet
     */
    Optional<ProductImage> findByStyleCodeAndColourAndCoverTrue(String styleCode, String colour);
}
