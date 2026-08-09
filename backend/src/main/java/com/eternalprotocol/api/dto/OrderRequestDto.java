package com.eternalprotocol.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * One checkout = one or more items, one delivery address, one optional
 * discount code, one PayHere payment. Replaces the old single-productId
 * shape — a cart with 2 tees + 1 in qty 3 is now ONE OrderRequestDto with
 * a 2-element `items` list (quantity lives on each item, not the order).
 */
public record OrderRequestDto(
        @NotBlank(message = "Customer name is required") String customerName,
        @NotBlank(message = "Phone number is required") String phone,
        @NotBlank(message = "Delivery address is required") String address,
        @NotEmpty(message = "At least one item is required") @Valid List<OrderItemRequestDto> items,
        String athleteCode // optional
) {}
