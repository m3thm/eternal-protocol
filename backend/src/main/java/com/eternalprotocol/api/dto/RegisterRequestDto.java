package com.eternalprotocol.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for a customer creating a new account.
 *
 * @param name     customer's full name
 * @param email    login email, must be a valid email format
 * @param password login password, at least 6 characters
 * @param phone    contact number for delivery
 * @param address  delivery address
 */
public record RegisterRequestDto(
        @NotBlank(message = "Name is required") String name,
        @NotBlank(message = "Email is required") @Email(message = "Enter a valid email") String email,
        @NotBlank(message = "Password is required") @Size(min = 6, message = "Password must be at least 6 characters") String password,
        @NotBlank(message = "Phone number is required") String phone,
        @NotBlank(message = "Address is required") String address
) {}
