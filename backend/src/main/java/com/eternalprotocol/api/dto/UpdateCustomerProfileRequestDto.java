package com.eternalprotocol.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Deliberately excludes email and password — changing your login email
 * needs a uniqueness re-check and changing your password needs
 * current-password verification, both bigger features than "edit my
 * delivery details." This covers what a customer needs day to day.
 */
public record UpdateCustomerProfileRequestDto(
        @NotBlank(message = "Name is required") String name,
        @NotBlank(message = "Phone number is required") String phone,
        @NotBlank(message = "Address is required") String address
) {}
