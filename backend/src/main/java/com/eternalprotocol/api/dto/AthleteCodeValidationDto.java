package com.eternalprotocol.api.dto;

import java.math.BigDecimal;

public record AthleteCodeValidationDto(
        boolean valid,
        String code,
        BigDecimal discountPercentage
) {
    public static AthleteCodeValidationDto invalid(String code) {
        return new AthleteCodeValidationDto(false, code, BigDecimal.ZERO);
    }
}
