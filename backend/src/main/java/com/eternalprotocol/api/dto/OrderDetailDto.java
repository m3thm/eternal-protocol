package com.eternalprotocol.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Full details of a single order, used for the order confirmation page and
 * a customer's order history.
 *
 * @param id              order's database ID
 * @param items           products purchased in this order
 * @param subtotalAmount  total before discount
 * @param discountAmount  amount discounted
 * @param totalAmount     amount actually paid
 * @param paymentStatus   current payment status, e.g. "PAID"
 * @param athleteCode     athlete code used, or null if none
 * @param orderDate       when the order was placed
 */
public record OrderDetailDto(
        Long id,
        List<OrderLineItemDto> items,
        BigDecimal subtotalAmount,
        BigDecimal discountAmount,
        BigDecimal totalAmount,
        String paymentStatus,
        String athleteCode,
        LocalDateTime orderDate
) {}
