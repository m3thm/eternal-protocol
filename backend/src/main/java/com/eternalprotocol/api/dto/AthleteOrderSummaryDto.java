package com.eternalprotocol.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * One order as shown in an athlete's own dashboard, including the
 * commission it earned.
 *
 * @param orderId           order's database ID
 * @param items             products purchased in this order
 * @param orderDate         when the order was placed
 * @param paymentStatus     current payment status, e.g. "PAID"
 * @param commissionAmount  commission earned from this order
 * @param commissionStatus  current commission status, e.g. "EARNED"
 */
public record AthleteOrderSummaryDto(
        Long orderId,
        List<OrderLineItemDto> items,
        LocalDateTime orderDate,
        String paymentStatus,
        BigDecimal commissionAmount,
        String commissionStatus
) {}
