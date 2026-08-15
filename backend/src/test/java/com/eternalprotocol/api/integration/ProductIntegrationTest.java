package com.eternalprotocol.api.integration;

import com.eternalprotocol.api.dto.ProductRequestDto;
import com.eternalprotocol.api.entity.ProductCategory;
import com.eternalprotocol.api.repository.AdminRepository;
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
class ProductIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private AdminRepository adminRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken(String email) throws Exception {
        return createAdminAndGetToken(mockMvc, objectMapper, adminRepository, passwordEncoder, email, "adminpass123");
    }

    @Test
    void getAllProducts_noAuthRequired_returnsCreatedProduct() throws Exception {
        // No more DataSeeder guaranteeing a product exists — create one
        // directly so this test is self-contained regardless of what else
        // is in the DB.
        String adminToken = adminToken("product.list.admin@example.com");
        ProductRequestDto request = new ProductRequestDto(
                "LIST-TEST", "Listing Test Tee", ProductCategory.MENS, "T-Shirts",
                new BigDecimal("2000.00"), "Grey", "M", 10, null, null);
        mockMvc.perform(post("/api/products")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").exists());
    }

    @Test
    void getProductById_unknownId_returns404WithConsistentErrorShape() throws Exception {
        mockMvc.perform(get("/api/products/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void createProduct_noToken_returns401() throws Exception {
        ProductRequestDto request = new ProductRequestDto(
                "TEST-STYLE", "New Tee", ProductCategory.MENS, "T-Shirts",
                new BigDecimal("3000.00"), "White", "L", 20, null, null);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createProduct_customerToken_returns403() throws Exception {
        // A logged-in customer is authenticated, just not authorized for
        // this route — should be 403, not 401.
        String token = registerCustomerAndGetToken(mockMvc, objectMapper, "product.customer@example.com");
        ProductRequestDto request = new ProductRequestDto(
                "TEST-STYLE", "New Tee", ProductCategory.MENS, "T-Shirts",
                new BigDecimal("3000.00"), "White", "L", 20, null, null);

        mockMvc.perform(post("/api/products")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createProduct_adminToken_succeedsAndProductIsRetrievable() throws Exception {
        String adminToken = adminToken("product.create.admin@example.com");
        ProductRequestDto request = new ProductRequestDto(
                "TEST-STYLE", "Integration Test Tee", ProductCategory.MENS, "T-Shirts",
                new BigDecimal("3000.00"), "White", "L", 20,
                "https://example.com/img.png", "A test product");

        String responseJson = mockMvc.perform(post("/api/products")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Integration Test Tee"))
                .andExpect(jsonPath("$.styleCode").value("TEST-STYLE"))
                .andExpect(jsonPath("$.category").value("MENS"))
                .andExpect(jsonPath("$.subCategory").value("T-Shirts"))
                .andReturn().getResponse().getContentAsString();

        Long newProductId = objectMapper.readTree(responseJson).get("id").asLong();

        mockMvc.perform(get("/api/products/" + newProductId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Integration Test Tee"));
    }

    @Test
    void updateProduct_adminToken_persistsChanges() throws Exception {
        String adminToken = adminToken("product.update.admin@example.com");
        ProductRequestDto createRequest = new ProductRequestDto(
                "TEST-STYLE", "Original Name", ProductCategory.MENS, "T-Shirts",
                new BigDecimal("1000.00"), "Red", "S", 5, null, null);

        String createResponse = mockMvc.perform(post("/api/products")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn().getResponse().getContentAsString();
        Long productId = objectMapper.readTree(createResponse).get("id").asLong();

        ProductRequestDto updateRequest = new ProductRequestDto(
                "TEST-STYLE", "Renamed", ProductCategory.WOMENS, "Hoodies",
                new BigDecimal("1200.00"), "Blue", "M", 8, null, null);

        mockMvc.perform(put("/api/products/" + productId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed"))
                .andExpect(jsonPath("$.colour").value("Blue"))
                .andExpect(jsonPath("$.category").value("WOMENS"))
                .andExpect(jsonPath("$.subCategory").value("Hoodies"));
    }

    @Test
    void deleteProduct_adminToken_removesIt() throws Exception {
        String adminToken = adminToken("product.delete.admin@example.com");
        ProductRequestDto createRequest = new ProductRequestDto(
                "TEST-STYLE", "To Be Deleted", ProductCategory.ACCESSORIES, "Caps",
                new BigDecimal("500.00"), "Black", "S", 1, null, null);

        String createResponse = mockMvc.perform(post("/api/products")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn().getResponse().getContentAsString();
        Long productId = objectMapper.readTree(createResponse).get("id").asLong();

        mockMvc.perform(delete("/api/products/" + productId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/products/" + productId))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAllProducts_styleCodeFilter_returnsOnlyMatchingVariants() throws Exception {
        String adminToken = adminToken("product.filter.admin@example.com");
        String styleCode = "GROUPING-TEST-" + System.nanoTime(); // unique per run, avoids clashing with seed data

        ProductRequestDto blackVariant = new ProductRequestDto(
                styleCode, "Grouping Test Tee", ProductCategory.MENS, "T-Shirts",
                new BigDecimal("2500.00"), "Black", "M", 10, null, null);
        ProductRequestDto whiteVariant = new ProductRequestDto(
                styleCode, "Grouping Test Tee", ProductCategory.MENS, "T-Shirts",
                new BigDecimal("2500.00"), "White", "M", 10, null, null);

        mockMvc.perform(post("/api/products")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blackVariant)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/products")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(whiteVariant)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/products").param("styleCode", styleCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].styleCode").value(styleCode))
                .andExpect(jsonPath("$[1].styleCode").value(styleCode));
    }

    @Test
    void createProduct_missingStyleCode_returns400() throws Exception {
        String adminToken = adminToken("product.missingstyle.admin@example.com");
        String jsonMissingStyleCode = """
                {"name":"No Style Code Tee","price":1000.00,"colour":"Black","size":"M","stockQty":5}
                """;

        mockMvc.perform(post("/api/products")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMissingStyleCode))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createProduct_missingRequiredFields_returns400() throws Exception {
        String adminToken = adminToken("product.missingfields.admin@example.com");
        String malformedJson = """
                {"name":"","price":null,"colour":"","size":"","stockQty":null}
                """;

        mockMvc.perform(post("/api/products")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createProduct_missingCategory_returns400() throws Exception {
        // category is @NotNull — an admin submitting the product form
        // without selecting MEN'S/WOMEN'S/ACCESSORIES should get a clear
        // validation error rather than the product silently landing with
        // no category and never appearing in any navbar section.
        String adminToken = adminToken("product.missingcategory.admin@example.com");
        String jsonMissingCategory = """
                {"styleCode":"NO-CATEGORY","name":"No Category Tee","subCategory":"T-Shirts",
                 "price":1000.00,"colour":"Black","size":"M","stockQty":5}
                """;

        mockMvc.perform(post("/api/products")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMissingCategory))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllProducts_categoryFilter_returnsOnlyMatchingCategory() throws Exception {
        String adminToken = adminToken("product.categoryfilter.admin@example.com");
        String mensStyle = "MENS-FILTER-" + System.nanoTime();
        String womensStyle = "WOMENS-FILTER-" + System.nanoTime();

        ProductRequestDto mensProduct = new ProductRequestDto(
                mensStyle, "Mens Filter Tee", ProductCategory.MENS, "T-Shirts",
                new BigDecimal("2500.00"), "Black", "M", 10, null, null);
        ProductRequestDto womensProduct = new ProductRequestDto(
                womensStyle, "Womens Filter Hoodie", ProductCategory.WOMENS, "Hoodies",
                new BigDecimal("4500.00"), "Pink", "S", 10, null, null);

        mockMvc.perform(post("/api/products")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mensProduct)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/products")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(womensProduct)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/products").param("styleCode", mensStyle).param("category", "MENS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].styleCode").value(mensStyle));

        // Requesting the wrong category for that styleCode should filter it out entirely.
        mockMvc.perform(get("/api/products").param("styleCode", mensStyle).param("category", "WOMENS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getAllProducts_subCategoryFilter_returnsOnlyMatchingSubCategory() throws Exception {
        String adminToken = adminToken("product.subcategoryfilter.admin@example.com");
        String styleCode = "SUBCAT-FILTER-" + System.nanoTime();

        ProductRequestDto product = new ProductRequestDto(
                styleCode, "Subcategory Filter Cap", ProductCategory.ACCESSORIES, "Caps",
                new BigDecimal("1500.00"), "Black", "One Size", 10, null, null);

        mockMvc.perform(post("/api/products")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(product)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/products").param("styleCode", styleCode).param("subCategory", "Caps"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/api/products").param("styleCode", styleCode).param("subCategory", "Bags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getSubCategories_returnsDistinctSubCategoriesForThatCategoryOnly() throws Exception {
        String adminToken = adminToken("product.subcategories.admin@example.com");
        String suffix = String.valueOf(System.nanoTime());

        ProductRequestDto cap = new ProductRequestDto(
                "CAP-" + suffix, "Test Cap", ProductCategory.ACCESSORIES, "Caps-" + suffix,
                new BigDecimal("1500.00"), "Black", "One Size", 10, null, null);
        ProductRequestDto bag = new ProductRequestDto(
                "BAG-" + suffix, "Test Bag", ProductCategory.ACCESSORIES, "Bags-" + suffix,
                new BigDecimal("3500.00"), "Black", "One Size", 10, null, null);
        ProductRequestDto tee = new ProductRequestDto(
                "TEE-" + suffix, "Test Tee", ProductCategory.MENS, "T-Shirts-" + suffix,
                new BigDecimal("2500.00"), "Black", "M", 10, null, null);

        for (ProductRequestDto p : new ProductRequestDto[]{cap, bag, tee}) {
            mockMvc.perform(post("/api/products")
                            .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(p)))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(get("/api/products/subcategories").param("category", "ACCESSORIES"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasItems("Caps-" + suffix, "Bags-" + suffix)))
                .andExpect(jsonPath("$", org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("T-Shirts-" + suffix))));
    }

    @Test
    void getSubCategories_noAuthRequired() throws Exception {
        mockMvc.perform(get("/api/products/subcategories").param("category", "MENS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // ---- image upload: access control only (no real Cloudinary call — see ImageUploadServiceTest for upload logic) ----

    @Test
    void uploadImage_noToken_returns401() throws Exception {
        org.springframework.mock.web.MockMultipartFile file = new org.springframework.mock.web.MockMultipartFile(
                "file", "tee.png", "image/png", "fake-bytes".getBytes());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .multipart("/api/products/upload-image").file(file))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void uploadImage_customerToken_returns403() throws Exception {
        String customerToken = registerCustomerAndGetToken(mockMvc, objectMapper, "product.upload.customer@example.com");
        org.springframework.mock.web.MockMultipartFile file = new org.springframework.mock.web.MockMultipartFile(
                "file", "tee.png", "image/png", "fake-bytes".getBytes());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .multipart("/api/products/upload-image").file(file)
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerToken)))
                .andExpect(status().isForbidden());
    }
}
