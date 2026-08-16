package com.eternalprotocol.api.exception;

/**
 * Thrown when an order or cart action would need more units of a product
 * than are currently in stock. Caught by {@link GlobalExceptionHandler} and
 * turned into a 409 Conflict response.
 */
public class InsufficientStockException extends RuntimeException {

    /**
     * @param message explanation shown to the caller
     */
    public InsufficientStockException(String message) {
        super(message);
    }
}
