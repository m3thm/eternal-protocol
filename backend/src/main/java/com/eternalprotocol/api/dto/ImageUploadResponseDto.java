package com.eternalprotocol.api.dto;

/**
 * Response returned after successfully uploading an image to Cloudinary.
 *
 * @param imageUrl public URL of the uploaded image, used to display it
 * @param publicId Cloudinary's own asset ID, kept so a later delete can remove the exact asset instead of guessing at it from the URL
 */
public record ImageUploadResponseDto(String imageUrl, String publicId) {}
