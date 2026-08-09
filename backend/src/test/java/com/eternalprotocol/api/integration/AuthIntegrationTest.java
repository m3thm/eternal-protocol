package com.eternalprotocol.api.integration;

import com.eternalprotocol.api.dto.LoginRequestDto;
import com.eternalprotocol.api.dto.RegisterRequestDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Full-stack: real HTTP requests through Spring MVC, Spring Security,
 * AuthService, and an H2 database — nothing mocked. Each test runs inside
 * a transaction that's rolled back afterward, so tests can't pollute each
 * other's data even though they share the same app context/database.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void register_newCustomer_returns200WithTokenAndCustomerRole() throws Exception {
        RegisterRequestDto request = new RegisterRequestDto(
                "Jane Doe", "jane.register@example.com", "password123", "0771234567", "123 Main St");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.userId").isNumber());
    }

    @Test
    void register_thenLogin_succeedsWithSameCredentials() throws Exception {
        RegisterRequestDto registerRequest = new RegisterRequestDto(
                "Jane Doe", "jane.login@example.com", "password123", "0771234567", "123 Main St");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk());

        LoginRequestDto loginRequest = new LoginRequestDto("jane.login@example.com", "password123");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        RegisterRequestDto request = new RegisterRequestDto(
                "Jane Doe", "jane.dup@example.com", "password123", "0771234567", "123 Main St");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("An account with this email already exists"));
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        RegisterRequestDto registerRequest = new RegisterRequestDto(
                "Jane Doe", "jane.wrongpw@example.com", "password123", "0771234567", "123 Main St");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk());

        LoginRequestDto loginRequest = new LoginRequestDto("jane.wrongpw@example.com", "totallywrong");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_unknownEmail_returns401() throws Exception {
        LoginRequestDto loginRequest = new LoginRequestDto("nobody-like-this-exists@example.com", "whatever");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_seededAdminCredentials_returns200WithAdminRole() throws Exception {
        // Confirms DataSeeder's default admin login actually works end to
        // end — this is the account your group will use to demo the admin
        // dashboard, so it's worth its own explicit test.
        LoginRequestDto loginRequest = new LoginRequestDto("admin@eternalprotocol.com", "admin123");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void register_missingRequiredField_returns400() throws Exception {
        String malformedJson = """
                {"name":"Jane Doe","email":"not-an-email","password":"123","phone":"","address":""}
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest());
    }
}
