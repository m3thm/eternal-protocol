package com.eternalprotocol.api.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

/**
 * Request body for an admin updating an existing athlete.
 * <p>
 * Both fields are nullable and optional — send only the field you want to
 * change, and leave the other one out to keep its current value unchanged.
 *
 * @param commissionRate new commission percentage, or null to leave unchanged
 * @param active         new active/inactive state, or null to leave unchanged
 */
public record UpdateAthleteRequestDto(
        @DecimalMin(value = "0.0", message = "Commission rate cannot be negative")
        @DecimalMax(value = "100.0", message = "Commission rate cannot exceed 100")
        BigDecimal commissionRate,
        Boolean active
) {}
