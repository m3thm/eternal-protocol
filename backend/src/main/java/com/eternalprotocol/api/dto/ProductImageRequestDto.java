package com.eternalprotocol.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Admin submits this after calling the existing /api/products/upload-image
 * endpoint to get an imageUrl — same two-step pattern as ProductRequestDto's
 * imageUrl field. sortOrder is optional; omit it to append to the end of
 * that colour's gallery.
 */
public record ProductImageRequestDto(
        @NotBlank(message = "Colour is required") String colour,
        @NotBlank(message = "Image URL is required") String imageUrl,
        String publicId,
        Integer sortOrder
) {}
