package com.eternalprotocol.api.dto;

import java.math.BigDecimal;

public record CartItemDto(
        Long id,
        Long productId,
        String productName,
        BigDecimal price,
        String size,
        String colour,
        Integer quantity,
        String imageUrl
) {}
