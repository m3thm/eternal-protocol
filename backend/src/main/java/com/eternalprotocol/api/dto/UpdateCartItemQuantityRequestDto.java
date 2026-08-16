package com.eternalprotocol.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for changing the quantity of one existing cart line.
 *
 * @param quantity new quantity, must be at least 1
 */
public record UpdateCartItemQuantityRequestDto(
        @NotNull(message = "Quantity is required") @Min(value = 1, message = "Quantity must be at least 1") Integer quantity
) {}
