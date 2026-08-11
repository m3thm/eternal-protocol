package com.eternalprotocol.api.integration;

import com.eternalprotocol.api.dto.OrderRequestDto;
import com.eternalprotocol.api.dto.ProductRequestDto;
import com.eternalprotocol.api.entity.Athlete;
import com.eternalprotocol.api.repository.AdminRepository;
import com.eternalprotocol.api.repository.AthleteRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static com.eternalprotocol.api.integration.AuthTestHelper.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AthleteIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private AthleteRepository athleteRepository;
    @Autowired
    private AdminRepository adminRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private Athlete seedAthlete(String code, String email, String discountPercent) {
        Athlete athlete = new Athlete();
        athlete.setName("Test Athlete " + code);
        athlete.setEmail(email);
        athlete.setPasswordHash(passwordEncoder.encode("password123"));
        athlete.setAthleteCode(code);
        athlete.setCommissionRate(new BigDecimal(discountPercent));
        athlete.setActive(true);
        return athleteRepository.save(athlete);
    }

    private Long createTestProduct() throws Exception {
        String adminToken = createAdminAndGetToken(mockMvc, objectMapper, adminRepository, passwordEncoder,
                "athlete.test.admin@example.com", "adminpass123");
        ProductRequestDto request = new ProductRequestDto(
                "ATHLETE-TEST", "Athlete Test Tee", new BigDecimal("4500.00"), "Black", "M", 20, null, null);
        String response = mockMvc.perform(post("/api/products")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    // ---- validate-code: public ----

    @Test
    void validateCode_activeAthlete_returnsValidWithDiscountRate() throws Exception {
        seedAthlete("VALID10", "valid10@example.com", "10.00");

        mockMvc.perform(get("/api/athletes/validate-code/VALID10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.discountPercentage").value(10.00));
    }

    @Test
    void validateCode_unknownCode_returnsInvalidNot404() throws Exception {
        // The checkout page calls this live while typing — it should
        // degrade gracefully to "invalid", not throw an error.
        mockMvc.perform(get("/api/athletes/validate-code/NOPE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false));
    }

    // ---- dashboard: access control ----

    @Test
    void dashboard_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/athletes/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void dashboard_customerToken_returns403() throws Exception {
        String customerToken = registerCustomerAndGetToken(mockMvc, objectMapper, "athlete.wrongrole@example.com");

        mockMvc.perform(get("/api/athletes/dashboard")
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void dashboard_validAthleteToken_returnsOwnData() throws Exception {
        seedAthlete("DASH01", "dash01@example.com", "10.00");
        String athleteToken = loginAndGetToken(mockMvc, objectMapper, "dash01@example.com", "password123");

        mockMvc.perform(get("/api/athletes/dashboard")
                        .header(HttpHeaders.AUTHORIZATION, bearer(athleteToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.athleteCode").value("DASH01"))
                .andExpect(jsonPath("$.totalOrders").value(0));
    }

    @Test
    void dashboard_twoAthletes_eachOnlySeesTheirOwnOrders() throws Exception {
        // This is the core security property called out throughout the
        // codebase: athlete ID always comes from the JWT, never a URL
        // param, so one athlete literally cannot see another's commissions.
        Long productId = createTestProduct();
        seedAthlete("ATHLETEA", "athletea@example.com", "10.00");
        seedAthlete("ATHLETEB", "athleteb@example.com", "10.00");

        // One order placed using Athlete A's code only.
        OrderRequestDto order = new OrderRequestDto(
                "Jane Doe", "0771234567", "123 Main St",
                java.util.List.of(new com.eternalprotocol.api.dto.OrderItemRequestDto(productId, "M", "Black", 1)),
                "ATHLETEA");
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(order)))
                .andExpect(status().isOk());

        String athleteAToken = loginAndGetToken(mockMvc, objectMapper, "athletea@example.com", "password123");
        String athleteBToken = loginAndGetToken(mockMvc, objectMapper, "athleteb@example.com", "password123");

        mockMvc.perform(get("/api/athletes/dashboard")
                        .header(HttpHeaders.AUTHORIZATION, bearer(athleteAToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOrders").value(1));

        mockMvc.perform(get("/api/athletes/dashboard")
                        .header(HttpHeaders.AUTHORIZATION, bearer(athleteBToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOrders").value(0)); // sees nothing from Athlete A's sale
    }

    @Test
    void dashboard_deactivatedAthlete_cannotLoginAtAll() throws Exception {
        Athlete athlete = seedAthlete("DEACTIVATED", "deactivated@example.com", "10.00");
        athlete.setActive(false);
        athleteRepository.save(athlete);

        String loginJson = objectMapper.writeValueAsString(
                new com.eternalprotocol.api.dto.LoginRequestDto("deactivated@example.com", "password123"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isUnauthorized());
    }
}
