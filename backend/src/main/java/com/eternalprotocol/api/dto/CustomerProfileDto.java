package com.eternalprotocol.api.dto;

public record CustomerProfileDto(
        Long id,
        String name,
        String email,
        String phone,
        String address
) {}
