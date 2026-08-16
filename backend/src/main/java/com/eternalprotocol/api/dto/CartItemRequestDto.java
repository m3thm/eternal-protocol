package com.eternalprotocol.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for adding an item to a logged-in customer's cart.
 * <p>
 * The {@code @NotNull}/{@code @NotBlank}/{@code @Min} annotations are
 * validation rules. Spring checks them automatically before this data ever
 * reaches the controller's own code, and rejects the request with the given
 * message if a rule is broken.
 *
 * @param productId product variant to add
 * @param size      size chosen
 * @param colour    colour chosen
 * @param quantity  how many units, must be at least 1
 */
public record CartItemRequestDto(
        @NotNull(message = "Product is required") Long productId,
        @NotBlank(message = "Size is required") String size,
        @NotBlank(message = "Colour is required") String colour,
        @NotNull(message = "Quantity is required") @Min(value = 1, message = "Quantity must be at least 1") Integer quantity
) {}
