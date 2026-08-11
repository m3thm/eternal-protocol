package com.eternalprotocol.api.integration;

import com.eternalprotocol.api.dto.LoginRequestDto;
import com.eternalprotocol.api.dto.RegisterRequestDto;
import com.eternalprotocol.api.entity.Admin;
import com.eternalprotocol.api.repository.AdminRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    @Autowired
    private AdminRepository adminRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

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
    void login_adminCredentials_returns200WithAdminRole() throws Exception {
        // No longer relies on a global seeder creating a fixed admin login —
        // that account doesn't exist by default any more (see AdminBootstrap,
        // which only creates one from ADMIN_BOOTSTRAP_EMAIL/PASSWORD if set).
        // This test creates its own admin directly so it stays self-contained
        // regardless of what's configured in .env.
        Admin admin = new Admin();
        admin.setName("Test Admin");
        admin.setEmail("admin.test@example.com");
        admin.setPasswordHash(passwordEncoder.encode("adminpass123"));
        adminRepository.save(admin);

        LoginRequestDto loginRequest = new LoginRequestDto("admin.test@example.com", "adminpass123");
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
