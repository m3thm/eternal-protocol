package com.eternalprotocol.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * One product line submitted as part of placing an order — see
 * {@link OrderRequestDto}, which contains a list of these.
 *
 * @param productId product variant being purchased
 * @param size      size chosen
 * @param colour    colour chosen
 * @param quantity  how many units, must be at least 1
 */
public record OrderItemRequestDto(
        @NotNull(message = "Product is required") Long productId,
        @NotBlank(message = "Size is required") String size,
        @NotBlank(message = "Colour is required") String colour,
        @NotNull(message = "Quantity is required") @Min(value = 1, message = "Quantity must be at least 1") Integer quantity
) {}
