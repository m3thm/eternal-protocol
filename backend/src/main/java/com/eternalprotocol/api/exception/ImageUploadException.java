package com.eternalprotocol.api.exception;

/** Thrown when Cloudinary itself fails or is unreachable — distinct from
 *  IllegalArgumentException, which covers the file failing our own
 *  validation (too large, wrong type) before we even attempt the upload. */
public class ImageUploadException extends RuntimeException {
    public ImageUploadException(String message, Throwable cause) {
        super(message, cause);
    }
}
