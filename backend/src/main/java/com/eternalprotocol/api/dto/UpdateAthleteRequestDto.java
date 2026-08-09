package com.eternalprotocol.api.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

public record UpdateAthleteRequestDto(
        @DecimalMin(value = "0.0", message = "Commission rate cannot be negative")
        @DecimalMax(value = "100.0", message = "Commission rate cannot exceed 100")
        BigDecimal commissionRate, // nullable — omit to leave unchanged
        Boolean active             // nullable — omit to leave unchanged
) {}
