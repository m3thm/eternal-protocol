package com.eternalprotocol.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * One order as shown in the admin dashboard's order list.
 * <p>
 * This is a "DTO" (Data Transfer Object) — a simple class used only to
 * shape data sent to or from the API, separate from the database
 * {@code Order} entity. Using DTOs means the JSON sent to the frontend can
 * be shaped exactly how it's needed, without exposing raw database
 * structure or accidentally leaking fields that shouldn't be public.
 * <p>
 * This is a Java {@code record} — a compact way to declare an immutable
 * class that just holds data, where each parameter below automatically
 * becomes a read-only field with a matching getter.
 *
 * @param id              order's database ID
 * @param customerName    name on the order
 * @param customerPhone   phone number on the order
 * @param customerAddress delivery address
 * @param items           products purchased in this order
 * @param subtotalAmount  total before discount
 * @param discountAmount  amount discounted
 * @param totalAmount     amount actually paid
 * @param paymentStatus   current payment status, e.g. "PAID"
 * @param athleteCode     athlete code used, or null if none
 * @param orderDate       when the order was placed
 */
public record AdminOrderDto(
        Long id,
        String customerName,
        String customerPhone,
        String customerAddress,
        List<OrderLineItemDto> items,
        BigDecimal subtotalAmount,
        BigDecimal discountAmount,
        BigDecimal totalAmount,
        String paymentStatus,
        String athleteCode,
        LocalDateTime orderDate
) {}
