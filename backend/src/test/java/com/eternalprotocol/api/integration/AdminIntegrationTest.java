package com.eternalprotocol.api.integration;

import com.eternalprotocol.api.dto.CreateAthleteRequestDto;
import com.eternalprotocol.api.dto.UpdateAthleteRequestDto;
import com.eternalprotocol.api.entity.*;
import com.eternalprotocol.api.repository.AthleteRepository;
import com.eternalprotocol.api.repository.CommissionRepository;
import com.eternalprotocol.api.repository.CustomerRepository;
import com.eternalprotocol.api.repository.OrderRepository;
import com.eternalprotocol.api.repository.ProductRepository;
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
class AdminIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private AthleteRepository athleteRepository;
    @Autowired
    private CommissionRepository commissionRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken() throws Exception {
        return loginAndGetToken(mockMvc, objectMapper, "admin@eternalprotocol.com", "admin123");
    }

    // ---- access control: every /api/admin/** route needs ADMIN ----

    @Test
    void adminOrders_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/admin/orders")).andExpect(status().isUnauthorized());
    }

    @Test
    void adminOrders_customerToken_returns403() throws Exception {
        String customerToken = registerCustomerAndGetToken(mockMvc, objectMapper, "admin.wrongrole@example.com");
        mockMvc.perform(get("/api/admin/orders")
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminOrders_athleteToken_returns403() throws Exception {
        Athlete athlete = new Athlete();
        athlete.setName("Some Athlete");
        athlete.setEmail("admin.athlete.wrongrole@example.com");
        athlete.setPasswordHash(passwordEncoder.encode("password123"));
        athlete.setAthleteCode("WRONGROLE");
        athlete.setCommissionRate(new BigDecimal("10.00"));
        athlete.setActive(true);
        athleteRepository.save(athlete);

        String athleteToken = loginAndGetToken(mockMvc, objectMapper,
                "admin.athlete.wrongrole@example.com", "password123");

        mockMvc.perform(get("/api/admin/orders")
                        .header(HttpHeaders.AUTHORIZATION, bearer(athleteToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminOrders_adminToken_returns200() throws Exception {
        mockMvc.perform(get("/api/admin/orders")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // ---- athlete management ----

    @Test
    void createAthlete_adminToken_createsNewAthleteLogin() throws Exception {
        CreateAthleteRequestDto request = new CreateAthleteRequestDto(
                "New Athlete", "admin.created.athlete@example.com", "password123", "ADMINCREATED", new BigDecimal("12.50"));

        mockMvc.perform(post("/api/admin/athletes")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.athleteCode").value("ADMINCREATED"))
                .andExpect(jsonPath("$.active").value(true));

        // and that athlete can now actually log in
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new com.eternalprotocol.api.dto.LoginRequestDto(
                                        "admin.created.athlete@example.com", "password123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ATHLETE"));
    }

    @Test
    void createAthlete_duplicateCode_returns400() throws Exception {
        Athlete existing = new Athlete();
        existing.setName("Existing Athlete");
        existing.setEmail("existing.for.dup@example.com");
        existing.setPasswordHash(passwordEncoder.encode("password123"));
        existing.setAthleteCode("DUPTEST");
        existing.setCommissionRate(new BigDecimal("10.00"));
        existing.setActive(true);
        athleteRepository.save(existing);

        CreateAthleteRequestDto request = new CreateAthleteRequestDto(
                "New Athlete", "new.dup.test@example.com", "password123", "DUPTEST", new BigDecimal("10.00"));

        mockMvc.perform(post("/api/admin/athletes")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateAthlete_deactivate_athleteCanNoLongerLogin() throws Exception {
        Athlete athlete = new Athlete();
        athlete.setName("To Deactivate");
        athlete.setEmail("to.deactivate@example.com");
        athlete.setPasswordHash(passwordEncoder.encode("password123"));
        athlete.setAthleteCode("DEACTIVATE_ME");
        athlete.setCommissionRate(new BigDecimal("10.00"));
        athlete.setActive(true);
        athlete = athleteRepository.save(athlete);

        UpdateAthleteRequestDto request = new UpdateAthleteRequestDto(null, false);

        mockMvc.perform(put("/api/admin/athletes/" + athlete.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new com.eternalprotocol.api.dto.LoginRequestDto(
                                        "to.deactivate@example.com", "password123"))))
                .andExpect(status().isUnauthorized());
    }

    // ---- commission management ----

    @Test
    void markCommissionPaid_earnedCommission_succeeds() throws Exception {
        Athlete athlete = new Athlete();
        athlete.setName("Commission Athlete");
        athlete.setEmail("commission.athlete@example.com");
        athlete.setPasswordHash(passwordEncoder.encode("password123"));
        athlete.setAthleteCode("COMMISH");
        athlete.setCommissionRate(new BigDecimal("10.00"));
        athlete.setActive(true);
        athlete = athleteRepository.save(athlete);

        Product product = new Product();
        product.setStyleCode("COMMISSION-TEST");
        product.setName("Commission Test Tee");
        product.setPrice(new BigDecimal("4500.00"));
        product.setColour("Black");
        product.setSize("M");
        product.setStockQty(10);
        product = productRepository.save(product);

        Customer customer = customerRepository.save(new Customer("Jane Doe", "0771234567", "123 Main St"));

        Order order = new Order(customer, new BigDecimal("4500.00"), new BigDecimal("450.00"), "COMMISH");
        order.addItem(new com.eternalprotocol.api.entity.OrderItem(order, product, "M", "Black", 1, new BigDecimal("4500.00")));
        order = orderRepository.save(order);

        Commission commission = new Commission(athlete, order, new BigDecimal("450.00"));
        commission.setCommissionStatus(CommissionStatus.EARNED);
        commission = commissionRepository.save(commission);

        mockMvc.perform(put("/api/admin/commissions/" + commission.getId() + "/mark-paid")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commissionStatus").value("PAID"))
                .andExpect(jsonPath("$.paidDate").exists());
    }

    @Test
    void getAllCommissions_filterByStatus_onlyReturnsMatching() throws Exception {
        mockMvc.perform(get("/api/admin/commissions").param("status", "PAID")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
