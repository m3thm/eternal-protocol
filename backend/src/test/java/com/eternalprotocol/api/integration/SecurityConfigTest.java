package com.eternalprotocol.api.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static com.eternalprotocol.api.integration.AuthTestHelper.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Specifically pins down the 401-vs-403 split configured in
 * SecurityConfig's exceptionHandling block: missing/garbage token -> 401,
 * valid token but wrong role -> 403. This distinction is easy to
 * accidentally regress (it's not Spring Security's default behavior — see
 * the comment in SecurityConfig), so it gets its own focused test class
 * rather than being spread thin across the other integration tests.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void protectedRoute_noAuthorizationHeaderAtAll_returns401WithConsistentErrorShape() throws Exception {
        mockMvc.perform(get("/api/admin/orders"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void protectedRoute_garbageToken_returns401() throws Exception {
        mockMvc.perform(get("/api/admin/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer this-is-not-a-real-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedRoute_validTokenWrongRole_returns403WithConsistentErrorShape() throws Exception {
        String customerToken = registerCustomerAndGetToken(mockMvc, objectMapper, "security.wrongrole@example.com");

        mockMvc.perform(get("/api/admin/orders")
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void protectedRoute_validTokenCorrectRole_succeeds() throws Exception {
        String adminToken = loginAndGetToken(mockMvc, objectMapper, "admin@eternalprotocol.com", "admin123");

        mockMvc.perform(get("/api/admin/orders")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk());
    }

    @Test
    void publicRoute_noTokenAtAll_stillSucceeds() throws Exception {
        // Sanity check that the 401 default doesn't accidentally leak onto
        // routes that are supposed to be public.
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk());
    }
}
