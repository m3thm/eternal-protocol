package com.eternalprotocol.api.exception;

/**
 * Thrown when a lookup by ID (or similar) finds nothing — e.g. fetching a
 * product, order, or user that doesn't exist. Caught by
 * {@link GlobalExceptionHandler} and turned into a 404 Not Found response.
 */
public class ResourceNotFoundException extends RuntimeException {

    /**
     * @param message explanation shown to the caller
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
