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

/**
 * Handles uploading and deleting product images on Cloudinary.
 * <p>
 * Image files themselves are never stored in this app's own database or
 * disk — only the resulting Cloudinary URL is saved, in
 * {@code ProductImage.imageUrl}.
 */
@Service
public class ImageUploadService {

    private static final Logger log = LoggerFactory.getLogger(ImageUploadService.class);
    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024; // 5MB
    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp", "image/gif");

    private final Cloudinary cloudinary;

    /**
     * @param cloudinary the shared Cloudinary client, configured in {@code CloudinaryConfig}
     */
    public ImageUploadService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    /**
     * Result of a successful upload.
     *
     * @param url      Cloudinary's permanent, public HTTPS URL — used to display the image
     * @param publicId Cloudinary's own asset ID — needed later to delete this exact image
     */
    public record UploadResult(String url, String publicId) {}

    /**
     * Validates and uploads a product image to Cloudinary.
     *
     * @param file the image file submitted by the admin
     * @return the uploaded image's URL and Cloudinary ID
     * @throws IllegalArgumentException if the file is missing, too large, or an unsupported type
     * @throws ImageUploadException     if Cloudinary itself fails to accept the upload
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
     * Deletes an image asset from Cloudinary, called whenever a gallery
     * photo is removed or a colour's whole photo set is cleaned up after
     * its last product row is deleted — so it isn't left burning storage
     * quota for no reason.
     * <p>
     * Deliberately does not throw on failure. By the time this runs, the
     * admin's actual intent (remove this image from the product listing)
     * has already succeeded in our own database, so a transient Cloudinary
     * error shouldn't roll that back or surface as a confusing error on an
     * otherwise-successful delete. Worst case is one orphaned Cloudinary
     * asset, which gets logged here for manual cleanup later.
     *
     * @param publicId Cloudinary's asset ID to delete, may be null/blank if unknown
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

    /**
     * Checks a file is present, under the size limit, and an allowed image
     * type before it's ever sent to Cloudinary.
     *
     * @param file the file to validate
     * @throws IllegalArgumentException if any check fails
     */
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
