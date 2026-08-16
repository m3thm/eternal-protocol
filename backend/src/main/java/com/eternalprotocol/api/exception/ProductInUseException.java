package com.eternalprotocol.api.exception;

/**
 * Thrown when a product can't be permanently deleted because it's
 * referenced by historical order data ({@code OrderItem} rows). Deleting it
 * anyway would either be blocked by the database's foreign-key constraint,
 * or worse, silently break past orders' records. Caught by
 * {@link GlobalExceptionHandler} and turned into a clear, actionable 409
 * Conflict response instead of a generic server error.
 */
public class ProductInUseException extends RuntimeException {

    /**
     * @param message explanation shown to the caller
     */
    public ProductInUseException(String message) {
        super(message);
    }
}
