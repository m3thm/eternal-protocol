package com.eternalprotocol.api.dto;

/** Returned by the image upload endpoint. imageUrl is what gets displayed;
 *  publicId is Cloudinary's own asset ID, kept so a later delete can remove
 *  the exact asset from Cloudinary instead of guessing at it from the URL. */
public record ImageUploadResponseDto(String imageUrl, String publicId) {}
