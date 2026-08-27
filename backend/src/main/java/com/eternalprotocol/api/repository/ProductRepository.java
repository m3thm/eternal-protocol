package com.eternalprotocol.api.repository;

import com.eternalprotocol.api.entity.Product;
import com.eternalprotocol.api.entity.ProductCategory;
import com.eternalprotocol.api.entity.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Data access for {@link Product} rows. Most methods here use
 * {@code @Query}, which lets us write the database query by hand (in JPQL,
 * a SQL-like language that works with entity classes instead of table
 * names) instead of relying on Spring to guess it from a method name. Each
 * {@code @Param} annotation binds a Java method parameter to a named
 * placeholder (e.g. {@code :styleCode}) inside that query.
 */
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Filters products by style code, colour, and/or size. Pass null for
     * any filter you don't want applied. Used internally by services (not
     * the public API) for lookups that already know the exact style, such
     * as syncing cover images or cleaning up a gallery when a product is
     * deleted — category doesn't matter for those, so it's left out of
     * this query and handled separately by {@link #searchStorefront}.
     *
     * @param styleCode style code to match, or null to ignore
     * @param colour    colour to match, or null to ignore
     * @param size      size to match, or null to ignore
     * @return products matching all the given filters
     */
    @Query("SELECT p FROM Product p WHERE " +
            "(:styleCode IS NULL OR LOWER(p.styleCode) = LOWER(:styleCode)) AND " +
            "(:colour IS NULL OR LOWER(p.colour) = LOWER(:colour)) AND " +
            "(:size IS NULL OR LOWER(p.size) = LOWER(:size))")
    List<Product> search(@Param("styleCode") String styleCode,
                          @Param("colour") String colour,
                          @Param("size") String size);

    /**
     * The full storefront search backing {@code GET /api/products}. Same
     * filters as {@link #search}, plus category, subcategory, and status.
     * Pass null for any filter you don't want applied — the admin product
     * table relies on this to leave {@code status} unset and see every
     * product regardless of lifecycle state, while storefront pages pass
     * {@code ACTIVE} (or {@code COMING_SOON} for the Upcoming page)
     * explicitly. Newest first, so a "New Drops" feed is just the head of
     * this list rather than a separate query.
     *
     * @param styleCode   style code to match, or null to ignore
     * @param colour      colour to match, or null to ignore
     * @param size        size to match, or null to ignore
     * @param category    top-level category to match, or null to ignore
     * @param subCategory subcategory to match, or null to ignore
     * @param status      lifecycle status to match, or null to ignore
     * @return matching products, newest first
     */
    @Query("SELECT p FROM Product p WHERE " +
            "(:styleCode IS NULL OR LOWER(p.styleCode) = LOWER(:styleCode)) AND " +
            "(:colour IS NULL OR LOWER(p.colour) = LOWER(:colour)) AND " +
            "(:size IS NULL OR LOWER(p.size) = LOWER(:size)) AND " +
            "(:category IS NULL OR p.category = :category) AND " +
            "(:subCategory IS NULL OR LOWER(p.subCategory) = LOWER(:subCategory)) AND " +
            "(:status IS NULL OR p.status = :status) " +
            "ORDER BY p.createdAt DESC")
    List<Product> searchStorefront(@Param("styleCode") String styleCode,
                                    @Param("colour") String colour,
                                    @Param("size") String size,
                                    @Param("category") ProductCategory category,
                                    @Param("subCategory") String subCategory,
                                    @Param("status") ProductStatus status);

    /**
     * Gets every distinct subcategory name currently used within one
     * category, alphabetically. Powers the navbar's per-category dropdown,
     * so it only ever lists subcategories that actually have products
     * right now — the same way colour/size filter options are driven by
     * what's actually listed.
     *
     * @param category category to look up subcategories for
     * @return distinct subcategory names, alphabetical
     */
    @Query("SELECT DISTINCT p.subCategory FROM Product p WHERE p.category = :category " +
            "AND p.subCategory IS NOT NULL ORDER BY p.subCategory ASC")
    List<String> findDistinctSubCategoriesByCategory(@Param("category") ProductCategory category);
}
