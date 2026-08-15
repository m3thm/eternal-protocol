package com.eternalprotocol.api.repository;

import com.eternalprotocol.api.entity.Product;
import com.eternalprotocol.api.entity.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Search covering styleCode/colour/size only — what ProductService and
     * ProductImageService use internally for style+colour lookups (cover
     * sync, cascading gallery cleanup on delete) where category is
     * irrelevant, since those already scope by an exact styleCode. Kept
     * separate from {@link #searchStorefront} below so those internal call
     * sites don't need to pass category/subCategory nulls they don't care
     * about.
     */
    @Query("SELECT p FROM Product p WHERE " +
            "(:styleCode IS NULL OR LOWER(p.styleCode) = LOWER(:styleCode)) AND " +
            "(:colour IS NULL OR LOWER(p.colour) = LOWER(:colour)) AND " +
            "(:size IS NULL OR LOWER(p.size) = LOWER(:size))")
    List<Product> search(@Param("styleCode") String styleCode,
                          @Param("colour") String colour,
                          @Param("size") String size);

    /**
     * The public GET /api/products search: everything `search` covers,
     * plus the MEN'S/WOMEN'S/ACCESSORIES navbar sections and their
     * subcategory dropdowns. Pass null for any filter you don't want
     * applied.
     */
    @Query("SELECT p FROM Product p WHERE " +
            "(:styleCode IS NULL OR LOWER(p.styleCode) = LOWER(:styleCode)) AND " +
            "(:colour IS NULL OR LOWER(p.colour) = LOWER(:colour)) AND " +
            "(:size IS NULL OR LOWER(p.size) = LOWER(:size)) AND " +
            "(:category IS NULL OR p.category = :category) AND " +
            "(:subCategory IS NULL OR LOWER(p.subCategory) = LOWER(:subCategory))")
    List<Product> searchStorefront(@Param("styleCode") String styleCode,
                                    @Param("colour") String colour,
                                    @Param("size") String size,
                                    @Param("category") ProductCategory category,
                                    @Param("subCategory") String subCategory);

    /**
     * Distinct subcategory names within one category, for the navbar's
     * per-category dropdown — only ever shows subcategories that actually
     * have products right now, the same way colour/size filter options on
     * the shop page are driven by what's actually in stock/listed.
     */
    @Query("SELECT DISTINCT p.subCategory FROM Product p WHERE p.category = :category " +
            "AND p.subCategory IS NOT NULL ORDER BY p.subCategory ASC")
    List<String> findDistinctSubCategoriesByCategory(@Param("category") ProductCategory category);
}
