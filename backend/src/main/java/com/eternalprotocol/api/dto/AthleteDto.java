package com.eternalprotocol.api.dto;

import java.math.BigDecimal;

/**
 * One athlete as shown in the admin dashboard's athlete list.
 *
 * @param id              athlete's database ID
 * @param name            athlete's full name
 * @param email           athlete's login email
 * @param athleteCode     athlete's discount/referral code
 * @param commissionRate  athlete's commission percentage
 * @param active          whether this athlete's code can currently be used
 */
public record AthleteDto(
        Long id,
        String name,
        String email,
        String athleteCode,
        BigDecimal commissionRate,
        boolean active
) {}
