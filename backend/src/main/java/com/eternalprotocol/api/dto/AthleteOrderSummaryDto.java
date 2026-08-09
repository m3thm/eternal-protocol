package com.eternalprotocol.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AthleteOrderSummaryDto(
        Long orderId,
        List<OrderLineItemDto> items,
        LocalDateTime orderDate,
        String paymentStatus,
        BigDecimal commissionAmount,
        String commissionStatus
) {}
