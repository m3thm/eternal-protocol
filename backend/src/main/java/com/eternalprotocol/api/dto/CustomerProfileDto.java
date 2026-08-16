package com.eternalprotocol.api.dto;

/**
 * A logged-in customer's own profile details, as shown on their account
 * page.
 *
 * @param id      customer's database ID
 * @param name    customer's full name
 * @param email   customer's login email
 * @param phone   customer's contact number
 * @param address customer's delivery address
 */
public record CustomerProfileDto(
        Long id,
        String name,
        String email,
        String phone,
        String address
) {}
