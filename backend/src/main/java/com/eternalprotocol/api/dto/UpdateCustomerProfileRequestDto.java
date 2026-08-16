package com.eternalprotocol.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for a customer updating their own delivery details.
 * <p>
 * Deliberately excludes email and password: changing a login email needs
 * a uniqueness re-check, and changing a password needs current-password
 * verification — both bigger features than editing delivery details, so
 * they're left out of this simpler endpoint.
 *
 * @param name    updated name
 * @param phone   updated contact number
 * @param address updated delivery address
 */
public record UpdateCustomerProfileRequestDto(
        @NotBlank(message = "Name is required") String name,
        @NotBlank(message = "Phone number is required") String phone,
        @NotBlank(message = "Address is required") String address
) {}
