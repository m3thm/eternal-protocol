package com.eternalprotocol.api.integration;

import com.eternalprotocol.api.dto.CartItemRequestDto;
import com.eternalprotocol.api.dto.ProductRequestDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static com.eternalprotocol.api.integration.AuthTestHelper.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CustomerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private Long createTestProduct() throws Exception {
        String adminToken = loginAndGetToken(mockMvc, objectMapper, "admin@eternalprotocol.com", "admin123");
        ProductRequestDto request = new ProductRequestDto(
                "CART-TEST", "Cart Test Tee", new BigDecimal("2000.00"), "Green", "M", 20, null, null);
        String response = mockMvc.perform(post("/api/products")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    // ---- access control ----

    @Test
    void profile_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/customers/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void profile_adminToken_returns403() throws Exception {
        String adminToken = loginAndGetToken(mockMvc, objectMapper, "admin@eternalprotocol.com", "admin123");
        mockMvc.perform(get("/api/customers/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void profile_validCustomerToken_returnsOwnProfile() throws Exception {
        String token = registerCustomerAndGetToken(mockMvc, objectMapper, "profile.test@example.com");

        mockMvc.perform(get("/api/customers/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("profile.test@example.com"))
                .andExpect(jsonPath("$.name").value("Test Customer"));
    }

    // ---- profile editing ----

    @Test
    void updateProfile_noToken_returns401() throws Exception {
        com.eternalprotocol.api.dto.UpdateCustomerProfileRequestDto request =
                new com.eternalprotocol.api.dto.UpdateCustomerProfileRequestDto("New Name", "0779999999", "New Address");

        mockMvc.perform(put("/api/customers/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateProfile_validToken_persistsChangesAndLeavesEmailUntouched() throws Exception {
        String token = registerCustomerAndGetToken(mockMvc, objectMapper, "profile.update@example.com");
        com.eternalprotocol.api.dto.UpdateCustomerProfileRequestDto request =
                new com.eternalprotocol.api.dto.UpdateCustomerProfileRequestDto(
                        "Updated Name", "0779999999", "456 New Street");

        mockMvc.perform(put("/api/customers/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.phone").value("0779999999"))
                .andExpect(jsonPath("$.address").value("456 New Street"))
                .andExpect(jsonPath("$.email").value("profile.update@example.com")); // unchanged — not editable here

        // Confirm it actually persisted, not just echoed back in the response.
        mockMvc.perform(get("/api/customers/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"));
    }

    @Test
    void updateProfile_missingRequiredField_returns400() throws Exception {
        String token = registerCustomerAndGetToken(mockMvc, objectMapper, "profile.invalid@example.com");
        String malformedJson = """
                {"name":"","phone":"","address":""}
                """;

        mockMvc.perform(put("/api/customers/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest());
    }

    // ---- persisted cart ----

    @Test
    void addToCart_thenGetCart_returnsTheItem() throws Exception {
        Long productId = createTestProduct();
        String token = registerCustomerAndGetToken(mockMvc, objectMapper, "cart.add@example.com");
        CartItemRequestDto request = new CartItemRequestDto(productId, "M", "Green", 2);

        mockMvc.perform(post("/api/customers/me/cart")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(2));

        mockMvc.perform(get("/api/customers/me/cart")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productId").value(productId))
                .andExpect(jsonPath("$[0].quantity").value(2));
    }

    @Test
    void addToCart_sameItemTwice_incrementsQuantityRatherThanDuplicating() throws Exception {
        Long productId = createTestProduct();
        String token = registerCustomerAndGetToken(mockMvc, objectMapper, "cart.increment@example.com");
        CartItemRequestDto request = new CartItemRequestDto(productId, "M", "Green", 1);

        mockMvc.perform(post("/api/customers/me/cart")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/customers/me/cart")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/customers/me/cart")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1)) // still one line
                .andExpect(jsonPath("$[0].quantity").value(2)); // quantity summed
    }

    @Test
    void cart_twoCustomers_eachOnlySeesTheirOwnItems() throws Exception {
        Long productId = createTestProduct();
        String tokenA = registerCustomerAndGetToken(mockMvc, objectMapper, "cart.customerA@example.com");
        String tokenB = registerCustomerAndGetToken(mockMvc, objectMapper, "cart.customerB@example.com");

        CartItemRequestDto request = new CartItemRequestDto(productId, "M", "Green", 1);
        mockMvc.perform(post("/api/customers/me/cart")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/customers/me/cart")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty()); // customer B sees nothing from A's cart
    }

    @Test
    void removeFromCart_anotherCustomersItemId_returns404NotSomeoneElsesItem() throws Exception {
        Long productId = createTestProduct();
        String tokenA = registerCustomerAndGetToken(mockMvc, objectMapper, "cart.owner@example.com");
        String tokenB = registerCustomerAndGetToken(mockMvc, objectMapper, "cart.attacker@example.com");

        CartItemRequestDto request = new CartItemRequestDto(productId, "M", "Green", 1);
        String addResponse = mockMvc.perform(post("/api/customers/me/cart")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();
        Long itemId = objectMapper.readTree(addResponse).get("id").asLong();

        // Customer B tries to delete customer A's cart item by guessing the ID.
        mockMvc.perform(delete("/api/customers/me/cart/" + itemId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenB)))
                .andExpect(status().isNotFound());

        // Confirm it's still there for the rightful owner.
        mockMvc.perform(get("/api/customers/me/cart")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void removeFromCart_ownItem_deletesSuccessfully() throws Exception {
        Long productId = createTestProduct();
        String token = registerCustomerAndGetToken(mockMvc, objectMapper, "cart.remove@example.com");

        CartItemRequestDto request = new CartItemRequestDto(productId, "M", "Green", 1);
        String addResponse = mockMvc.perform(post("/api/customers/me/cart")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();
        Long itemId = objectMapper.readTree(addResponse).get("id").asLong();

        mockMvc.perform(delete("/api/customers/me/cart/" + itemId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/customers/me/cart")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ---- cart quantity update (set, not increment) ----

    @Test
    void updateCartItemQuantity_ownItem_setsExactQuantity() throws Exception {
        Long productId = createTestProduct();
        String token = registerCustomerAndGetToken(mockMvc, objectMapper, "cart.setqty@example.com");

        CartItemRequestDto addRequest = new CartItemRequestDto(productId, "M", "Green", 1);
        String addResponse = mockMvc.perform(post("/api/customers/me/cart")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addRequest)))
                .andReturn().getResponse().getContentAsString();
        Long itemId = objectMapper.readTree(addResponse).get("id").asLong();

        com.eternalprotocol.api.dto.UpdateCartItemQuantityRequestDto updateRequest =
                new com.eternalprotocol.api.dto.UpdateCartItemQuantityRequestDto(7);

        mockMvc.perform(put("/api/customers/me/cart/" + itemId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(7)); // set to 7, not 1+7
    }

    @Test
    void updateCartItemQuantity_anotherCustomersItem_returns404() throws Exception {
        Long productId = createTestProduct();
        String tokenA = registerCustomerAndGetToken(mockMvc, objectMapper, "cart.setqty.owner@example.com");
        String tokenB = registerCustomerAndGetToken(mockMvc, objectMapper, "cart.setqty.attacker@example.com");

        CartItemRequestDto addRequest = new CartItemRequestDto(productId, "M", "Green", 1);
        String addResponse = mockMvc.perform(post("/api/customers/me/cart")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addRequest)))
                .andReturn().getResponse().getContentAsString();
        Long itemId = objectMapper.readTree(addResponse).get("id").asLong();

        com.eternalprotocol.api.dto.UpdateCartItemQuantityRequestDto updateRequest =
                new com.eternalprotocol.api.dto.UpdateCartItemQuantityRequestDto(99);

        mockMvc.perform(put("/api/customers/me/cart/" + itemId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateCartItemQuantity_zeroOrNegative_returns400() throws Exception {
        Long productId = createTestProduct();
        String token = registerCustomerAndGetToken(mockMvc, objectMapper, "cart.setqty.invalid@example.com");

        CartItemRequestDto addRequest = new CartItemRequestDto(productId, "M", "Green", 1);
        String addResponse = mockMvc.perform(post("/api/customers/me/cart")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addRequest)))
                .andReturn().getResponse().getContentAsString();
        Long itemId = objectMapper.readTree(addResponse).get("id").asLong();

        String zeroQuantityJson = """
                {"quantity":0}
                """;

        mockMvc.perform(put("/api/customers/me/cart/" + itemId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(zeroQuantityJson))
                .andExpect(status().isBadRequest());
    }
}
