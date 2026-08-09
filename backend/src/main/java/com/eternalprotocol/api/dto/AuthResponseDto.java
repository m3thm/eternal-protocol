package com.eternalprotocol.api.dto;

/** One shape returned by both /login and /register, regardless of which of
 *  the three tables (Customer/Athlete/Admin) the user actually lives in. */
public record AuthResponseDto(String token, String role, Long userId) {}
