package com.eternalprotocol.api.dto;

import java.math.BigDecimal;

/**
 * Response for checking whether an athlete code is currently usable at
 * checkout, and what discount it gives.
 *
 * @param valid              true if the code exists and is currently active
 * @param code               the code that was checked
 * @param discountPercentage discount percentage the code gives, zero if invalid
 */
public record AthleteCodeValidationDto(
        boolean valid,
        String code,
        BigDecimal discountPercentage
) {
    /**
     * Builds a response for a code that doesn't exist or isn't active.
     *
     * @param code the code that was checked
     * @return an invalid result with zero discount
     */
    public static AthleteCodeValidationDto invalid(String code) {
        return new AthleteCodeValidationDto(false, code, BigDecimal.ZERO);
    }
}
