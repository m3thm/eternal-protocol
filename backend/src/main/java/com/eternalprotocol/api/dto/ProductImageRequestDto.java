package com.eternalprotocol.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for attaching an already-uploaded image to a product
 * style/colour's gallery.
 * <p>
 * The admin uploads the file first via {@code POST /api/products/upload-image}
 * to get back an {@code imageUrl}, then sends that URL here to save it
 * against the product — a two-step process, upload then attach.
 *
 * @param colour    colour this image belongs to
 * @param imageUrl  URL returned by the upload endpoint
 * @param publicId  Cloudinary's asset ID for this image, optional
 * @param sortOrder position in the gallery; omit to append to the end of that colour's gallery
 */
public record ProductImageRequestDto(
        @NotBlank(message = "Colour is required") String colour,
        @NotBlank(message = "Image URL is required") String imageUrl,
        String publicId,
        Integer sortOrder
) {}
