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

/**
 * REST endpoints for placing orders and handling PayHere payment.
 * <p>
 * These endpoints are public (no login required) since guest checkout must
 * always work, but they also check for an optional logged-in customer so
 * an order can still be linked to an account when one exists.
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final PaymentService paymentService;

    /**
     * @param orderService   creates orders and calculates totals/discounts
     * @param paymentService verifies PayHere payment signatures
     */
    public OrderController(OrderService orderService, PaymentService paymentService) {
        this.orderService = orderService;
        this.paymentService = paymentService;
    }

    /**
     * Places a new order (checkout). Works for guests and logged-in
     * customers alike: if the request carries a valid customer JWT, the
     * order is linked to that account and appears in their order history;
     * otherwise a fresh guest customer record is created for just this
     * order.
     *
     * @param request        checkout details: items, delivery info, optional athlete code
     * @param authentication the current request's login info, if any
     * @return the created order's ID, total, and PayHere payment URL
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

    /**
     * Gets full details of one order, used for the order confirmation page.
     *
     * @param id order's database ID
     * @return the order's full details
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrderDetailDto> getOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderDetail(id));
    }

    /**
     * Returns a small self-submitting HTML page that redirects the
     * customer's browser straight into PayHere's payment form. The
     * frontend sends the browser here right after checkout completes.
     *
     * @param id order to pay for
     * @return an HTML page that auto-submits to PayHere
     */
    @GetMapping(value = "/{id}/checkout-redirect", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> checkoutRedirect(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.buildCheckoutRedirectHtml(id));
    }

    /**
     * PayHere's server-to-server payment webhook. Called directly by
     * PayHere itself (not by this app's own frontend) once a payment
     * finishes. The md5 signature is verified before the "payment
     * succeeded" claim is trusted — without that check, anyone could send
     * a fake success request here.
     *
     * @param id              order this payment is for
     * @param merchantId      PayHere merchant ID from the callback
     * @param orderId         this app's order reference, echoed back by PayHere
     * @param payhereAmount   amount PayHere recorded as paid
     * @param payhereCurrency currency PayHere recorded
     * @param statusCode      PayHere's payment result code ("2" = success)
     * @param md5sig          PayHere's signature, used to verify the callback is genuine
     * @return empty 200 response, always — PayHere just needs an acknowledgement
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
