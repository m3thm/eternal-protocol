package com.eternalprotocol.api.dto;

import java.math.BigDecimal;

public record AthleteDto(
        Long id,
        String name,
        String email,
        String athleteCode,
        BigDecimal commissionRate,
        boolean active
) {}
