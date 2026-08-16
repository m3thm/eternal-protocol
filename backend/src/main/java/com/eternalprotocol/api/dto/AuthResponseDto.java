package com.eternalprotocol.api.dto;

/**
 * Response returned after a successful login or registration.
 * <p>
 * The same shape is used no matter which of the three account types
 * (Customer, Athlete, Admin) the user belongs to — {@code role} tells the
 * frontend which one it's dealing with.
 *
 * @param token  JWT the frontend must send on future requests
 * @param role   "CUSTOMER", "ATHLETE", or "ADMIN"
 * @param userId the logged-in user's database ID
 */
public record AuthResponseDto(String token, String role, Long userId) {}
