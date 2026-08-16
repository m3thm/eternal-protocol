package com.eternalprotocol.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request body for logging in. Works for customers, athletes, and admins —
 * the backend checks all three account tables using this same email and
 * password.
 *
 * @param email    login email, must be a valid email format
 * @param password login password
 */
public record LoginRequestDto(
        @NotBlank(message = "Email is required") @Email(message = "Enter a valid email") String email,
        @NotBlank(message = "Password is required") String password
) {}
