package com.eternalprotocol.api.exception;

/**
 * Thrown when a signup attempt uses an email that's already registered.
 * Caught by {@link GlobalExceptionHandler} and turned into a 409 Conflict
 * response.
 */
public class EmailAlreadyInUseException extends RuntimeException {

    /**
     * @param message explanation shown to the caller
     */
    public EmailAlreadyInUseException(String message) {
        super(message);
    }
}
