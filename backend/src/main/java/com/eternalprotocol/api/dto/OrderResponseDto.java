package com.eternalprotocol.api.dto;

import java.math.BigDecimal;

/**
 * Response returned right after an order is placed, used to send the
 * customer on to PayHere for payment.
 *
 * @param orderId     newly created order's ID
 * @param totalAmount amount to be paid
 * @param paymentUrl  URL the frontend should redirect the customer to for payment
 */
public record OrderResponseDto(
        Long orderId,
        BigDecimal totalAmount,
        String paymentUrl
) {}
