package com.eternalprotocol.api.dto;

import java.math.BigDecimal;

/**
 * One line of an order, as read back after it's been placed. This shape is
 * reused across every view that needs to show what was bought —
 * {@link OrderDetailDto} (customer/guest confirmation), {@link AdminOrderDto}
 * (admin order table), and {@link AthleteOrderSummaryDto} (athlete
 * dashboard) — instead of each one defining its own slightly different
 * shape.
 *
 * @param productId   purchased product's ID
 * @param productName purchased product's name
 * @param size        size purchased
 * @param colour      colour purchased
 * @param quantity    quantity purchased
 * @param unitPrice   price per unit at time of purchase
 * @param lineTotal   unitPrice × quantity
 */
public record OrderLineItemDto(
        Long productId,
        String productName,
        String size,
        String colour,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {}
