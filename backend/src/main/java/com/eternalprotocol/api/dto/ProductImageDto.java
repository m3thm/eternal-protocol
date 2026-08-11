package com.eternalprotocol.api.dto;

public record ProductImageDto(
        Long id,
        String colour,
        String imageUrl,
        Integer sortOrder,
        boolean isCover
) {}
