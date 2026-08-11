package com.eternalprotocol.api.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.eternalprotocol.api.exception.ImageUploadException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

@Service
public class ImageUploadService {

    private static final Logger log = LoggerFactory.getLogger(ImageUploadService.class);
    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024; // 5MB
    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp", "image/gif");

    private final Cloudinary cloudinary;

    public ImageUploadService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    /** Cloudinary's `secure_url` (what gets displayed) plus its `public_id` (what a later delete needs). */
    public record UploadResult(String url, String publicId) {}

    /**
     * Validates and uploads a product image, returning its public,
     * permanent HTTPS URL plus the Cloudinary asset ID needed to delete it
     * later — the actual image bytes never touch our own database or disk.
     */
    public UploadResult uploadProductImage(MultipartFile file) {
        validate(file);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap("folder", "eternal-protocol/products"));
            return new UploadResult((String) uploadResult.get("secure_url"), (String) uploadResult.get("public_id"));
        } catch (IOException e) {
            throw new ImageUploadException(
                    "Failed to upload image — please try again", e);
        }
    }

    /**
     * Deletes an asset from Cloudinary by its public_id — called whenever a
     * gallery photo is removed, a colour's whole photo set is cleaned up
     * after its last product row is deleted, so it isn't sitting there
     * burning storage/bandwidth quota for nothing.
     * <p>
     * Deliberately swallows failures rather than throwing: the admin's
     * actual intent (remove this from my database / product listing) has
     * already succeeded by the time this runs, and a transient Cloudinary
     * error shouldn't roll that back or surface as a confusing error on an
     * otherwise-successful delete. Worst case on failure is one orphaned
     * Cloudinary asset, logged here for manual cleanup — not a broken app.
     */
    public void deleteProductImage(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            return;
        }
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (Exception e) {
            log.warn("Failed to delete Cloudinary asset '{}': {}", publicId, e.getMessage());
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("No image file provided");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("Image must be smaller than 5MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException(
                    "Only JPEG, PNG, WEBP, or GIF images are allowed");
        }
    }
}
