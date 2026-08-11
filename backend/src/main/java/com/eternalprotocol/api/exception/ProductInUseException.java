package com.eternalprotocol.api.exception;

/**
 * Thrown when a Product can't be hard-deleted because it's referenced by
 * historical order data (OrderItem rows). Deleting it would either be
 * blocked by the DB's foreign-key constraint or, worse, silently corrupt
 * past orders — so this is surfaced as a clear, actionable 409 instead of
 * a generic 500.
 */
public class ProductInUseException extends RuntimeException {
    public ProductInUseException(String message) {
        super(message);
    }
}
