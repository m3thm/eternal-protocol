package com.eternalprotocol.api.integration;

import com.eternalprotocol.api.dto.AuthResponseDto;
import com.eternalprotocol.api.dto.LoginRequestDto;
import com.eternalprotocol.api.dto.RegisterRequestDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Shared helper so every integration test class doesn't reimplement
 * "register a customer and grab the JWT" from scratch.
 */
class AuthTestHelper {

    static String registerCustomerAndGetToken(MockMvc mockMvc, ObjectMapper objectMapper, String email) throws Exception {
        RegisterRequestDto request = new RegisterRequestDto(
                "Test Customer", email, "password123", "0771234567", "123 Test St");
        String responseJson = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(responseJson, AuthResponseDto.class).token();
    }

    static String loginAndGetToken(MockMvc mockMvc, ObjectMapper objectMapper, String email, String password) throws Exception {
        LoginRequestDto request = new LoginRequestDto(email, password);
        String responseJson = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(responseJson, AuthResponseDto.class).token();
    }

    static String bearer(String token) {
        return "Bearer " + token;
    }
}
