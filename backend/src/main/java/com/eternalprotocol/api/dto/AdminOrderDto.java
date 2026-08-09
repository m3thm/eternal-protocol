package com.eternalprotocol.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AdminOrderDto(
        Long id,
        String customerName,
        String customerPhone,
        String customerAddress,
        List<OrderLineItemDto> items,
        BigDecimal subtotalAmount,
        BigDecimal discountAmount,
        BigDecimal totalAmount,
        String paymentStatus,
        String athleteCode,
        LocalDateTime orderDate
) {}
