package com.eternalprotocol.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CommissionDto(
        Long id,
        String athleteName,
        String athleteCode,
        Long orderId,
        BigDecimal commissionAmount,
        String commissionStatus,
        LocalDateTime paidDate
) {}
