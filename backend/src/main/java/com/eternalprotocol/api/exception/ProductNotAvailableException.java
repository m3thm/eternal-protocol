package com.eternalprotocol.api.exception;

/**
 * Thrown when a cart or order action targets a product that isn't
 * purchasable yet — currently, any product with
 * {@link com.eternalprotocol.api.entity.ProductStatus#COMING_SOON}.
 * Independent of {@link InsufficientStockException}: a coming-soon product
 * is blocked regardless of its stock count. Caught by
 * {@link GlobalExceptionHandler} and turned into a 409 Conflict response.
 */
public class ProductNotAvailableException extends RuntimeException {

    /**
     * @param message explanation shown to the caller
     */
    public ProductNotAvailableException(String message) {
        super(message);
    }
}
