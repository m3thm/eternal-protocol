package com.eternalprotocol.api.controller;

import com.eternalprotocol.api.dto.OrderDetailDto;
import com.eternalprotocol.api.dto.OrderRequestDto;
import com.eternalprotocol.api.dto.OrderResponseDto;
import com.eternalprotocol.api.security.AppUserDetails;
import com.eternalprotocol.api.service.OrderService;
import com.eternalprotocol.api.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final PaymentService paymentService;

    public OrderController(OrderService orderService, PaymentService paymentService) {
        this.orderService = orderService;
        this.paymentService = paymentService;
    }

    /**
     * Public — works for guest checkout. If the request happens to carry a
     * valid customer JWT (frontend's axiosClient attaches it automatically
     * whenever a token exists), the order gets linked to that account and
     * shows up in their order history; otherwise a fresh guest Customer row
     * is created, exactly as before accounts existed.
     */
    @PostMapping
    public ResponseEntity<OrderResponseDto> createOrder(@Valid @RequestBody OrderRequestDto request,
                                                          Authentication authentication) {
        Long loggedInCustomerId = null;
        if (authentication != null && authentication.getPrincipal() instanceof AppUserDetails user
                && "CUSTOMER".equals(user.getRole())) {
            loggedInCustomerId = user.getId();
        }
        return ResponseEntity.ok(orderService.createOrder(request, loggedInCustomerId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDetailDto> getOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderDetail(id));
    }

    /** Public — the browser is sent here right after checkout; it auto-submits to PayHere. */
    @GetMapping(value = "/{id}/checkout-redirect", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> checkoutRedirect(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.buildCheckoutRedirectHtml(id));
    }

    /**
     * PayHere's server-to-server webhook (notify_url), called directly by
     * PayHere — not by our own frontend. Verifies the md5 signature before
     * trusting the "payment succeeded" claim; anyone could otherwise POST a
     * fake success here.
     */
    @PostMapping("/{id}/confirm-payment")
    public ResponseEntity<Void> confirmPayment(
            @PathVariable Long id,
            @RequestParam("merchant_id") String merchantId,
            @RequestParam("order_id") String orderId,
            @RequestParam("payhere_amount") String payhereAmount,
            @RequestParam("payhere_currency") String payhereCurrency,
            @RequestParam("status_code") String statusCode,
            @RequestParam("md5sig") String md5sig) {

        boolean validSignature = paymentService.verifyNotifySignature(
                merchantId, orderId, payhereAmount, payhereCurrency, statusCode, md5sig);

        // PayHere status_code "2" = successfully completed.
        if (validSignature && "2".equals(statusCode)) {
            orderService.confirmPayment(id);
        }

        return ResponseEntity.ok().build();
    }
}
