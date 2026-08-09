package com.eternalprotocol.api.dto;

import java.math.BigDecimal;

public record OrderResponseDto(
        Long orderId,
        BigDecimal totalAmount,
        String paymentUrl
) {}
