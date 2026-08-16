package com.eternalprotocol.api.dto;

import java.math.BigDecimal;

/**
 * One line of a logged-in customer's cart, as shown on the frontend.
 *
 * @param id          cart line's database ID
 * @param productId   product variant's ID
 * @param productName product's display name
 * @param price       product's current price
 * @param size        size chosen
 * @param colour      colour chosen
 * @param quantity    quantity in the cart
 * @param imageUrl    product's thumbnail image
 */
public record CartItemDto(
        Long id,
        Long productId,
        String productName,
        BigDecimal price,
        String size,
        String colour,
        Integer quantity,
        String imageUrl
) {}
