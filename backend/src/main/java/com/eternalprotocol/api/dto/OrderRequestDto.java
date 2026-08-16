package com.eternalprotocol.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Request body for placing an order (checkout).
 * <p>
 * One checkout is one or more items, one delivery address, one optional
 * discount code, and one PayHere payment. For example, a cart with 2 tees
 * and 1 hoodie (quantity 3) is a single {@code OrderRequestDto} with a
 * 2-element {@code items} list — quantity lives on each item, not on the
 * order as a whole. {@code @Valid} on the list tells Spring to also check
 * each individual {@link OrderItemRequestDto} inside it.
 *
 * @param customerName name for delivery
 * @param phone        contact number for delivery
 * @param address      delivery address
 * @param items        products being purchased, at least one required
 * @param athleteCode  optional discount code
 */
public record OrderRequestDto(
        @NotBlank(message = "Customer name is required") String customerName,
        @NotBlank(message = "Phone number is required") String phone,
        @NotBlank(message = "Delivery address is required") String address,
        @NotEmpty(message = "At least one item is required") @Valid List<OrderItemRequestDto> items,
        String athleteCode
) {}
