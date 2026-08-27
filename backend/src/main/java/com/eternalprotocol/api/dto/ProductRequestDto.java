package com.eternalprotocol.api.dto;

import com.eternalprotocol.api.entity.ProductCategory;
import com.eternalprotocol.api.entity.ProductStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Request body for an admin creating or updating one product variant.
 * <p>
 * Represents a single colour/size row — creating three sizes of the same
 * style means submitting this three times with the same {@code styleCode}.
 *
 * @param styleCode   code shared by every colour/size of this style
 * @param name        product name shown to customers
 * @param category    top-level section
 * @param subCategory admin-set subcategory
 * @param price       price, must be greater than 0. Still required even for a
 *                     COMING_SOON product — whether it's actually displayed to
 *                     shoppers is controlled separately by {@code showPrice}
 * @param colour      colour of this variant
 * @param size        size of this variant. Required for MENS/WOMENS; may be
 *                     left blank for ACCESSORIES, which defaults to "One Size" —
 *                     see {@link com.eternalprotocol.api.factory.product.ProductCreator}
 * @param stockQty    units in stock, cannot be negative
 * @param imageUrl    main thumbnail image URL, obtained from the upload endpoint
 * @param description longer description text
 * @param status      lifecycle status; null defaults to ACTIVE (see {@code ProductCreator})
 * @param showPrice   whether to display the price; null defaults to true. Only
 *                     meaningful while status is COMING_SOON — an ACTIVE
 *                     product's price is always shown
 */
public record ProductRequestDto(
        @NotBlank(message = "Style code is required") String styleCode,
        @NotBlank(message = "Product name is required") String name,
        @NotNull(message = "Category is required") ProductCategory category,
        @NotBlank(message = "Subcategory is required") String subCategory,
        @NotNull(message = "Price is required") @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0") BigDecimal price,
        @NotBlank(message = "Colour is required") String colour,
        String size,
        @NotNull(message = "Stock quantity is required") @Min(value = 0, message = "Stock cannot be negative") Integer stockQty,
        String imageUrl,
        String description,
        ProductStatus status,
        Boolean showPrice
) {}
