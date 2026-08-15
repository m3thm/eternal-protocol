package com.eternalprotocol.api.dto;

import com.eternalprotocol.api.entity.ProductCategory;

import java.math.BigDecimal;

public record ProductDto(
        Long id,
        String styleCode,
        String name,
        ProductCategory category,
        String subCategory,
        BigDecimal price,
        String colour,
        String size,
        Integer stockQty,
        String imageUrl,
        String description
) {}
