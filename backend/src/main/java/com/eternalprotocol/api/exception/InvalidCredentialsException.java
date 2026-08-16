package com.eternalprotocol.api.exception;

/**
 * Thrown when a login attempt fails — wrong email or password. Caught by
 * {@link GlobalExceptionHandler} and turned into a 401 Unauthorized
 * response with a generic message, so callers can't tell whether the email
 * or the password was the wrong part.
 */
public class InvalidCredentialsException extends RuntimeException {

    /**
     * @param message explanation shown to the caller
     */
    public InvalidCredentialsException(String message) {
        super(message);
    }
}
