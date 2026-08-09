package com.eternalprotocol.api.dto;

import java.math.BigDecimal;

/**
 * One line of an order, as read back. Shared across every view that needs
 * to show what was actually bought — OrderDetailDto (customer/guest
 * confirmation), AdminOrderDto (admin order table), and the athlete
 * dashboard's per-order summary — rather than each defining its own
 * slightly-different shape.
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
