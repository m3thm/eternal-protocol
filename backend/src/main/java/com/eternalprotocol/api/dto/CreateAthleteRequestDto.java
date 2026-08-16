package com.eternalprotocol.api.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * Request body for an admin creating a new athlete account.
 *
 * @param name           athlete's full name
 * @param email          athlete's login email, must be a valid email format
 * @param password       athlete's login password, at least 6 characters
 * @param athleteCode    unique discount/referral code for this athlete
 * @param commissionRate commission percentage, between 0 and 100
 */
public record CreateAthleteRequestDto(
        @NotBlank(message = "Name is required") String name,
        @NotBlank(message = "Email is required") @Email(message = "Enter a valid email") String email,
        @NotBlank(message = "Password is required") @Size(min = 6, message = "Password must be at least 6 characters") String password,
        @NotBlank(message = "Athlete code is required") String athleteCode,
        @NotNull(message = "Commission rate is required")
        @DecimalMin(value = "0.0", message = "Commission rate cannot be negative")
        @DecimalMax(value = "100.0", message = "Commission rate cannot exceed 100")
        BigDecimal commissionRate
) {}
