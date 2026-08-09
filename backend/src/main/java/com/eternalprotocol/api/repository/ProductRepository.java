package com.eternalprotocol.api.repository;

import com.eternalprotocol.api.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Single flexible search covering every combination of filters the
     * storefront needs: browsing by style (all variants of one product),
     * filtering by colour/size on the shop grid, or any mix of the three.
     * Pass null for any filter you don't want applied.
     */
    @Query("SELECT p FROM Product p WHERE " +
            "(:styleCode IS NULL OR LOWER(p.styleCode) = LOWER(:styleCode)) AND " +
            "(:colour IS NULL OR LOWER(p.colour) = LOWER(:colour)) AND " +
            "(:size IS NULL OR LOWER(p.size) = LOWER(:size))")
    List<Product> search(@Param("styleCode") String styleCode,
                          @Param("colour") String colour,
                          @Param("size") String size);
}
