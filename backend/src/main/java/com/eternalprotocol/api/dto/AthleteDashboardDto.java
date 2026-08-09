package com.eternalprotocol.api.dto;

import java.math.BigDecimal;
import java.util.List;

public record AthleteDashboardDto(
        String athleteCode,
        BigDecimal commissionRate,
        int totalOrders,
        BigDecimal salesGenerated,
        BigDecimal commissionEarned,   // EARNED + PAID (i.e. payment confirmed)
        BigDecimal paidCommission,     // PAID only
        BigDecimal pendingCommission,  // EARNED but not yet PAID
        List<AthleteOrderSummaryDto> orders
) {}
