package com.eternalprotocol.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One commission as shown in the admin dashboard's commission list.
 *
 * @param id                commission's database ID
 * @param athleteName       name of the athlete who earned it
 * @param athleteCode       athlete's discount/referral code
 * @param orderId           order this commission came from
 * @param commissionAmount  amount owed to the athlete
 * @param commissionStatus  current status, e.g. "EARNED" or "PAID"
 * @param paidDate          date paid out, or null if not yet paid
 */
public record CommissionDto(
        Long id,
        String athleteName,
        String athleteCode,
        Long orderId,
        BigDecimal commissionAmount,
        String commissionStatus,
        LocalDateTime paidDate
) {}
