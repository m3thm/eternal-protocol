package com.eternalprotocol.api.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Summary data shown on an athlete's own dashboard: their performance and
 * commission totals.
 *
 * @param athleteCode       this athlete's discount/referral code
 * @param commissionRate    this athlete's commission percentage
 * @param totalOrders       number of orders placed with this athlete's code
 * @param salesGenerated    total value of sales generated
 * @param commissionEarned  commission earned so far — includes both EARNED and PAID (i.e. payment confirmed)
 * @param paidCommission    commission already paid out to the athlete
 * @param pendingCommission commission earned but not yet paid out
 * @param orders            list of individual orders behind these totals
 */
public record AthleteDashboardDto(
        String athleteCode,
        BigDecimal commissionRate,
        int totalOrders,
        BigDecimal salesGenerated,
        BigDecimal commissionEarned,
        BigDecimal paidCommission,
        BigDecimal pendingCommission,
        List<AthleteOrderSummaryDto> orders
) {}
