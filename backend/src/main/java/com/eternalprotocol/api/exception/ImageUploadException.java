package com.eternalprotocol.api.exception;

/**
 * Thrown when Cloudinary itself fails or can't be reached during an image
 * upload. Different from an {@link IllegalArgumentException}, which is used
 * when a file fails our own validation (too large, wrong file type) before
 * an upload to Cloudinary is even attempted. Caught by
 * {@link GlobalExceptionHandler} and turned into a 502 Bad Gateway
 * response, signalling that the problem is with the upstream image service,
 * not with this backend.
 */
public class ImageUploadException extends RuntimeException {

    /**
     * @param message explanation shown to the caller
     * @param cause   the original exception thrown by the upload attempt
     */
    public ImageUploadException(String message, Throwable cause) {
        super(message, cause);
    }
}
