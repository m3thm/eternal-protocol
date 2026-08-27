package com.eternalprotocol.api.integration;

import com.eternalprotocol.api.dto.OrderItemRequestDto;
import com.eternalprotocol.api.dto.OrderRequestDto;
import com.eternalprotocol.api.dto.ProductRequestDto;
import com.eternalprotocol.api.entity.Athlete;
import com.eternalprotocol.api.entity.ProductCategory;
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
import java.util.List;

import static com.eternalprotocol.api.integration.AuthTestHelper.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OrderIntegrationTest {

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

    // JUnit creates a fresh test instance per @Test method, so caching this
    // here is enough to keep createTestProduct's repeated admin logins
    // (some tests create two+ products) from trying to insert the same
    // admin email twice within one transaction.
    private String cachedAdminToken;

    private String adminToken() throws Exception {
        if (cachedAdminToken == null) {
            cachedAdminToken = createAdminAndGetToken(mockMvc, objectMapper, adminRepository, passwordEncoder,
                    "order.integration.admin@example.com", "adminpass123");
        }
        return cachedAdminToken;
    }

    /** Creates a fresh product via the admin endpoint so tests don't fight over stock on the seeded one. */
    private Long createTestProduct(String styleCode, String name, BigDecimal price, int stockQty) throws Exception {
        String adminToken = adminToken();
        ProductRequestDto request = new ProductRequestDto(
                styleCode, name, ProductCategory.MENS, "T-Shirts", price, "Black", "M", stockQty, null, null, null, null);

        String response = mockMvc.perform(post("/api/products")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private Long createTestProduct(int stockQty) throws Exception {
        return createTestProduct("ORDER-TEST", "Order Test Tee", new BigDecimal("4500.00"), stockQty);
    }

    private Athlete seedAthlete(String code, String discountPercent) {
        Athlete athlete = new Athlete();
        athlete.setName("Test Athlete");
        athlete.setEmail(code.toLowerCase() + "@example.com");
        athlete.setPasswordHash(passwordEncoder.encode("password123"));
        athlete.setAthleteCode(code);
        athlete.setCommissionRate(new BigDecimal(discountPercent));
        athlete.setActive(true);
        return athleteRepository.save(athlete);
    }

    @Test
    void createOrder_guestCheckoutNoCode_returns200WithFullPriceAndPaymentUrl() throws Exception {
        Long productId = createTestProduct(10);
        OrderRequestDto request = new OrderRequestDto(
                "Jane Doe", "0771234567", "123 Main St",
                List.of(new OrderItemRequestDto(productId, "M", "Black", 1)), null);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").isNumber())
                .andExpect(jsonPath("$.totalAmount").value(4500.00))
                .andExpect(jsonPath("$.paymentUrl").value(org.hamcrest.Matchers.containsString("/checkout-redirect")));
    }

    @Test
    void createOrder_decrementsProductStockByOrderedQuantity() throws Exception {
        Long productId = createTestProduct(5);
        OrderRequestDto request = new OrderRequestDto(
                "Jane Doe", "0771234567", "123 Main St",
                List.of(new OrderItemRequestDto(productId, "M", "Black", 2)), null);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/products/" + productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockQty").value(3)); // 5 - 2
    }

    @Test
    void createOrder_outOfStockProduct_returns409() throws Exception {
        Long productId = createTestProduct(0);
        OrderRequestDto request = new OrderRequestDto(
                "Jane Doe", "0771234567", "123 Main St",
                List.of(new OrderItemRequestDto(productId, "M", "Black", 1)), null);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Not enough stock")));
    }

    @Test
    void createOrder_validAthleteCode_appliesDiscountToWholeOrder() throws Exception {
        Long productId = createTestProduct(10);
        seedAthlete("TESTCODE10", "10.00");

        OrderRequestDto request = new OrderRequestDto(
                "Jane Doe", "0771234567", "123 Main St",
                List.of(new OrderItemRequestDto(productId, "M", "Black", 1)), "TESTCODE10");

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(4050.00)); // 4500 - 10%
    }

    @Test
    void createOrder_invalidAthleteCode_silentlyChargesFullPrice() throws Exception {
        Long productId = createTestProduct(10);
        OrderRequestDto request = new OrderRequestDto(
                "Jane Doe", "0771234567", "123 Main St",
                List.of(new OrderItemRequestDto(productId, "M", "Black", 1)), "DOESNOTEXIST");

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(4500.00));
    }

    @Test
    void createOrder_unknownProduct_returns404() throws Exception {
        OrderRequestDto request = new OrderRequestDto(
                "Jane Doe", "0771234567", "123 Main St",
                List.of(new OrderItemRequestDto(9999999L, "M", "Black", 1)), null);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void createOrder_emptyItemsList_returns400() throws Exception {
        String jsonWithEmptyItems = """
                {"customerName":"Jane Doe","phone":"0771234567","address":"123 Main St","items":[]}
                """;

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonWithEmptyItems))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getOrder_afterCheckout_returnsOrderDetailWithItems() throws Exception {
        Long productId = createTestProduct(10);
        OrderRequestDto request = new OrderRequestDto(
                "Jane Doe", "0771234567", "123 Main St",
                List.of(new OrderItemRequestDto(productId, "L", "White", 1)), null);

        String response = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();
        Long orderId = objectMapper.readTree(response).get("orderId").asLong();

        mockMvc.perform(get("/api/orders/" + orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].size").value("L"))
                .andExpect(jsonPath("$.items[0].colour").value("White"))
                .andExpect(jsonPath("$.paymentStatus").value("PENDING"));
    }

    // ---- the exact scenario from the multi-item bug report ----

    @Test
    void createOrder_cartWithTwoDistinctProductsAndAQuantityGreaterThanOne_createsOneOrderOnePayment() throws Exception {
        Long teeId = createTestProduct("STYLE-A", "Protocol 001 Tee", new BigDecimal("4500.00"), 10);
        Long hoodieId = createTestProduct("STYLE-B", "Protocol 002 Hoodie", new BigDecimal("8000.00"), 10);

        // 2 tees + 1 hoodie in qty 1 — mirrors "2 tees + 1 in qty 3" style scenario.
        OrderRequestDto request = new OrderRequestDto(
                "Jane Doe", "0771234567", "123 Main St",
                List.of(
                        new OrderItemRequestDto(teeId, "M", "Black", 2),
                        new OrderItemRequestDto(hoodieId, "L", "Black", 1)
                ), null);

        String response = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                // one order, one total, one payment URL for the WHOLE cart
                .andExpect(jsonPath("$.totalAmount").value(17000.00)) // (4500*2)+(8000*1)
                .andReturn().getResponse().getContentAsString();
        Long orderId = objectMapper.readTree(response).get("orderId").asLong();

        // Confirm it's genuinely one order with two line items, not two orders.
        mockMvc.perform(get("/api/orders/" + orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.subtotalAmount").value(17000.00))
                .andExpect(jsonPath("$.totalAmount").value(17000.00));

        // Both products' stock decremented independently and correctly.
        mockMvc.perform(get("/api/products/" + teeId))
                .andExpect(jsonPath("$.stockQty").value(8)); // 10 - 2
        mockMvc.perform(get("/api/products/" + hoodieId))
                .andExpect(jsonPath("$.stockQty").value(9)); // 10 - 1
    }

    @Test
    void createOrder_oneItemOutOfStockAmongMultiple_rejectsEntireOrder_leavesOtherStockUntouched() throws Exception {
        Long teeId = createTestProduct("STYLE-C", "In Stock Tee", new BigDecimal("4500.00"), 10);
        Long hoodieId = createTestProduct("STYLE-D", "Out Of Stock Hoodie", new BigDecimal("8000.00"), 0);

        OrderRequestDto request = new OrderRequestDto(
                "Jane Doe", "0771234567", "123 Main St",
                List.of(
                        new OrderItemRequestDto(teeId, "M", "Black", 1),
                        new OrderItemRequestDto(hoodieId, "L", "Black", 1)
                ), null);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());

        // The item that WAS in stock must not have been partially decremented.
        mockMvc.perform(get("/api/products/" + teeId))
                .andExpect(jsonPath("$.stockQty").value(10)); // untouched
    }

    @Test
    void checkoutRedirect_multiItemOrder_returnsAutoSubmittingPayHereForm() throws Exception {
        Long productId = createTestProduct(10);
        OrderRequestDto request = new OrderRequestDto(
                "Jane Doe", "0771234567", "123 Main St",
                List.of(new OrderItemRequestDto(productId, "M", "Black", 1)), null);

        String response = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();
        Long orderId = objectMapper.readTree(response).get("orderId").asLong();

        mockMvc.perform(get("/api/orders/" + orderId + "/checkout-redirect"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("payhereForm")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"order_id\" value=\"" + orderId + "\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "name=\"return_url\" value=\"http://localhost:5173/order/" + orderId + "/confirmation?status=success\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "name=\"cancel_url\" value=\"http://localhost:5173/order/" + orderId + "/confirmation?status=cancelled\"")));
    }

    @Test
    void createOrder_loggedInCustomer_appearsInTheirOrderHistoryAndClearsCart() throws Exception {
        Long productId = createTestProduct(10);
        String token = registerCustomerAndGetToken(mockMvc, objectMapper, "order.history@example.com");

        // Put something in the persisted cart first, to confirm checkout clears it.
        String cartItemJson = """
                {"productId":%d,"size":"M","colour":"Black","quantity":1}
                """.formatted(productId);
        mockMvc.perform(post("/api/customers/me/cart")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cartItemJson))
                .andExpect(status().isOk());

        OrderRequestDto request = new OrderRequestDto(
                "Jane Doe", "0771234567", "123 Main St",
                List.of(new OrderItemRequestDto(productId, "M", "Black", 1)), null);

        mockMvc.perform(post("/api/orders")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/customers/me/orders")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].items[0].size").value("M"));

        // Cart should now be empty — checkout clears it for logged-in customers.
        mockMvc.perform(get("/api/customers/me/cart")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void createOrder_guestCheckout_doesNotAppearInAnyCustomersOrderHistory() throws Exception {
        // A guest order creates its own throwaway Customer row with no
        // login credentials, so there's no account it could show up under.
        Long productId = createTestProduct(10);
        String token = registerCustomerAndGetToken(mockMvc, objectMapper, "order.noguest@example.com");

        OrderRequestDto guestRequest = new OrderRequestDto(
                "Guest Buyer", "0779999999", "999 Guest Ave",
                List.of(new OrderItemRequestDto(productId, "S", "Grey", 1)), null);
        mockMvc.perform(post("/api/orders") // no Authorization header at all
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(guestRequest)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/customers/me/orders")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }
}
