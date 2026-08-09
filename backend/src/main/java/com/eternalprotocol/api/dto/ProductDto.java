package com.eternalprotocol.api.dto;

import java.math.BigDecimal;

public record ProductDto(
        Long id,
        String styleCode,
        String name,
        BigDecimal price,
        String colour,
        String size,
        Integer stockQty,
        String imageUrl,
        String description
) {}
