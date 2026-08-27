package com.eternalprotocol.api.dto;

import com.eternalprotocol.api.entity.ProductCategory;
import com.eternalprotocol.api.entity.ProductStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A single product variant as shown on the storefront (shop grid and
 * product detail page).
 *
 * @param id          product variant's database ID
 * @param styleCode   code shared by every colour/size of this style
 * @param name        product name
 * @param category    top-level section, e.g. MENS
 * @param subCategory admin-set subcategory, e.g. "Hoodies"
 * @param price       price of this variant
 * @param colour      colour of this variant
 * @param size        size of this variant
 * @param stockQty    units currently in stock
 * @param imageUrl    main thumbnail image
 * @param description longer description text
 * @param status      lifecycle status — ACTIVE or COMING_SOON, independent of stock
 * @param showPrice   whether {@code price} should be displayed; only meaningful while COMING_SOON
 * @param createdAt   when this variant was first created, admin-immutable — drives the New Drops page
 */
public record ProductDto(
        Long id,
        String styleCode,
        String name,
        ProductCategory category,
        String subCategory,
        BigDecimal price,
        String colour,
        String size,
        Integer stockQty,
        String imageUrl,
        String description,
        ProductStatus status,
        boolean showPrice,
        LocalDateTime createdAt
) {}
