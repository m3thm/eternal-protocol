package com.eternalprotocol.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Central error handler for the whole API.
 * <p>
 * {@code @RestControllerAdvice} means this class automatically catches
 * exceptions thrown by any controller, without each controller needing its
 * own try/catch blocks. Every method here handles one type of exception and
 * turns it into the same consistent JSON shape:
 * <pre>{ "timestamp": "...", "status": 404, "error": "Not Found", "message": "..." }</pre>
 * so the frontend can handle every failure the same way, no matter which
 * endpoint threw it.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles a lookup that found nothing (e.g. product/order/user by ID).
     *
     * @param ex the thrown exception
     * @return 404 response
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /**
     * Handles failed logins, both our own exception and Spring Security's
     * built-in one, always with a generic message so callers can't tell
     * whether the email or the password was wrong.
     *
     * @param ex the thrown exception
     * @return 401 response
     */
    @ExceptionHandler({InvalidCredentialsException.class, BadCredentialsException.class})
    public ResponseEntity<Map<String, Object>> handleInvalidCredentials(RuntimeException ex) {
        return build(HttpStatus.UNAUTHORIZED, "Incorrect email or password");
    }

    /**
     * Handles signup with an email that's already registered.
     *
     * @param ex the thrown exception
     * @return 409 response
     */
    @ExceptionHandler(EmailAlreadyInUseException.class)
    public ResponseEntity<Map<String, Object>> handleEmailInUse(EmailAlreadyInUseException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    /**
     * Handles an order/cart action that needs more stock than is available.
     *
     * @param ex the thrown exception
     * @return 409 response
     */
    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<Map<String, Object>> handleInsufficientStock(InsufficientStockException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    /**
     * Handles an attempt to delete a product still referenced by past orders.
     *
     * @param ex the thrown exception
     * @return 409 response
     */
    @ExceptionHandler(ProductInUseException.class)
    public ResponseEntity<Map<String, Object>> handleProductInUse(ProductInUseException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    /**
     * Handles an attempt to add a COMING_SOON product to a cart or order.
     *
     * @param ex the thrown exception
     * @return 409 response
     */
    @ExceptionHandler(ProductNotAvailableException.class)
    public ResponseEntity<Map<String, Object>> handleProductNotAvailable(ProductNotAvailableException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    /**
     * Handles a failed image upload to Cloudinary.
     *
     * @param ex the thrown exception
     * @return 502 response — this server is fine, the upstream image service failed
     */
    @ExceptionHandler(ImageUploadException.class)
    public ResponseEntity<Map<String, Object>> handleImageUpload(ImageUploadException ex) {
        return build(HttpStatus.BAD_GATEWAY, ex.getMessage());
    }

    /**
     * Handles a file upload that's too large. Spring rejects an oversized
     * upload while parsing the request, before it ever reaches
     * {@code ImageUploadService}'s own size check — this gives the same
     * message, just caught one step earlier.
     *
     * @param ex the thrown exception
     * @return 400 response
     */
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUploadSize(
            org.springframework.web.multipart.MaxUploadSizeExceededException ex) {
        return build(HttpStatus.BAD_REQUEST, "Image must be smaller than 5MB");
    }

    /**
     * Fallback for any database constraint violation (foreign key, unique
     * constraint, etc.) that doesn't have its own specific handler above.
     * See {@code ProductService.deleteProduct} for the one place this kind
     * of error is caught separately with a more specific message. Still
     * treated as a client-side "you can't do that" error, not a server bug.
     *
     * @param ex the thrown exception
     * @return 409 response
     */
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(
            org.springframework.dao.DataIntegrityViolationException ex) {
        return build(HttpStatus.CONFLICT, "This action conflicts with existing related data and can't be completed.");
    }

    /**
     * Handles a logged-in user trying to do something their role doesn't allow.
     *
     * @param ex the thrown exception
     * @return 403 response
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, "You do not have permission to perform this action");
    }

    /**
     * Handles a request body that failed {@code @Valid} validation (e.g. a
     * missing required field in a DTO). Returns the first validation error
     * message found.
     *
     * @param ex the thrown exception
     * @return 400 response
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("Validation failed");
        return build(HttpStatus.BAD_REQUEST, message);
    }

    /**
     * Handles any other invalid input caught manually in service code.
     *
     * @param ex the thrown exception
     * @return 400 response
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * Last-resort catch-all for any exception not handled above, so the
     * caller never sees a raw stack trace or internal error detail.
     *
     * @param ex the thrown exception
     * @return 500 response with a generic message
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong. Please try again.");
    }

    /**
     * Builds the standard error response body shared by every handler above.
     *
     * @param status  HTTP status to return
     * @param message human-readable explanation
     * @return a ready-to-send error response
     */
    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}
