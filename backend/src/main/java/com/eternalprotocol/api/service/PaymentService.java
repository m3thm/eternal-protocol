package com.eternalprotocol.api.service;

import com.eternalprotocol.api.entity.Order;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.DecimalFormat;

/**
 * Wraps PayHere's hosted checkout integration.
 *
 * PayHere doesn't accept a simple GET redirect with query params — it
 * expects an HTML form POSTed to its checkout URL, including a hash
 * computed from the merchant secret (which must never reach the browser).
 * So "paymentUrl" returned to the frontend is actually a link to OUR OWN
 * backend endpoint (GET /api/orders/{id}/checkout-redirect), which serves a
 * tiny auto-submitting HTML form. The frontend's `window.location.href =
 * order.paymentUrl` from the frontend doc still works unmodified — it just
 * lands on our redirect page for a split second before PayHere.
 *
 * Docs: https://support.payhere.lk/api-&-mobile-sdk/payhere-checkout
 */
@Service
public class PaymentService {

    private final String merchantId;
    private final String merchantSecret;
    private final String checkoutUrl;
    private final String frontendBaseUrl;
    private final String appBaseUrl;

    public PaymentService(
            @Value("${app.payhere.merchant-id}") String merchantId,
            @Value("${app.payhere.merchant-secret}") String merchantSecret,
            @Value("${app.payhere.checkout-url}") String checkoutUrl,
            @Value("${app.frontend.base-url}") String frontendBaseUrl,
            @Value("${app.base-url}") String appBaseUrl) {
        this.merchantId = merchantId;
        this.merchantSecret = merchantSecret;
        this.checkoutUrl = checkoutUrl;
        this.frontendBaseUrl = frontendBaseUrl;
        this.appBaseUrl = appBaseUrl;
    }

    /** Called from OrderService right after an order is saved as PENDING. */
    public String initiatePayment(Order order, BigDecimal amount) {
        // The frontend just needs a URL to navigate to; the actual PayHere
        // form fields are built lazily when that URL is hit, so we never
        // have to store the hash anywhere.
        return appBaseUrl + "/api/orders/" + order.getId() + "/checkout-redirect";
    }

    /** Builds the auto-submitting HTML form that actually sends the customer to PayHere. */
    public String buildCheckoutFormHtml(Order order) {
        String orderId = String.valueOf(order.getId());
        String amount = formatAmount(order.getTotalAmount());
        String currency = "LKR";
        String hash = computeHash(orderId, amount, currency);

        String[] nameParts = splitName(order.getCustomer().getName());

        // notify_url, return_url, and cancel_url are all built per-order
        // (not fixed config values) so every redirect PayHere can trigger
        // knows exactly which order it's talking about.
        //
        // return_url and cancel_url share ONE frontend route
        // (/order/{id}/confirmation) with a `status` query param telling
        // the page which path the customer came back on — success or
        // cancelled. IMPORTANT: that `status` param is a UX hint only, not
        // proof of payment. A customer can reach return_url without ever
        // actually paying (e.g. manually typing the URL, or a slow/failed
        // webhook), so it must never be trusted as confirmation on its own.
        // The confirmation page should always call GET /api/orders/{id}
        // and read `paymentStatus` from there — that field only ever
        // flips to PAID via the signature-verified webhook in
        // confirmPayment(), which is the actual source of truth.
        String notifyUrl = appBaseUrl + "/api/orders/" + orderId + "/confirm-payment";
        String returnUrl = frontendBaseUrl + "/order/" + orderId + "/confirmation?status=success";
        String cancelUrl = frontendBaseUrl + "/order/" + orderId + "/confirmation?status=cancelled";

        return "<!DOCTYPE html><html><head><title>Redirecting to PayHere...</title></head><body>"
                + "<form id=\"payhereForm\" method=\"post\" action=\"" + checkoutUrl + "\">"
                + hidden("merchant_id", merchantId)
                + hidden("return_url", returnUrl)
                + hidden("cancel_url", cancelUrl)
                + hidden("notify_url", notifyUrl)
                + hidden("order_id", orderId)
                + hidden("items", escape(buildItemsSummary(order)))
                + hidden("currency", currency)
                + hidden("amount", amount)
                + hidden("hash", hash)
                + hidden("first_name", escape(nameParts[0]))
                + hidden("last_name", escape(nameParts[1]))
                + hidden("email", "customer@example.com")
                + hidden("phone", escape(order.getCustomer().getPhone()))
                + hidden("address", escape(order.getCustomer().getAddress()))
                + hidden("city", "Colombo")
                + hidden("country", "Sri Lanka")
                + "</form>"
                + "<p>Redirecting to secure payment...</p>"
                + "<script>document.getElementById('payhereForm').submit();</script>"
                + "</body></html>";
    }

    /**
     * Verifies the md5 signature PayHere sends on the server-to-server
     * notify_url webhook. Never trust a "payment confirmed" call without
     * checking this first — anyone could otherwise POST a fake success.
     */
    public boolean verifyNotifySignature(String merchantId, String orderId, String payhereAmount,
                                          String payhereCurrency, String statusCode, String md5sig) {
        String merchantSecretHash = md5Upper(merchantSecret);
        String localSig = md5Upper(merchantId + orderId + payhereAmount + payhereCurrency
                + statusCode + merchantSecretHash);
        return localSig.equalsIgnoreCase(md5sig);
    }

    private String computeHash(String orderId, String amount, String currency) {
        String merchantSecretHash = md5Upper(merchantSecret);
        return md5Upper(merchantId + orderId + amount + currency + merchantSecretHash);
    }

    private String md5Upper(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString().toUpperCase();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to compute PayHere hash", e);
        }
    }

    private String formatAmount(BigDecimal amount) {
        return new DecimalFormat("0.00").format(amount);
    }

    /**
     * PayHere's `items` field is just a display string on their checkout
     * page — no structured format required. Joins every line into
     * "Product Name x2, Other Product x1" so a multi-item order still
     * shows something meaningful there, capped at a reasonable length
     * since PayHere doesn't document a hard limit but very long strings
     * are a bad idea on a payment page regardless.
     */
    private String buildItemsSummary(Order order) {
        String summary = order.getItems().stream()
                .map(item -> item.getProduct().getName() + " x" + item.getQuantity())
                .collect(java.util.stream.Collectors.joining(", "));
        return summary.length() > 200 ? summary.substring(0, 197) + "..." : summary;
    }

    private String[] splitName(String fullName) {
        String[] parts = fullName.trim().split("\\s+", 2);
        return parts.length == 2 ? parts : new String[]{parts[0], "-"};
    }

    private String hidden(String name, String value) {
        return "<input type=\"hidden\" name=\"" + name + "\" value=\"" + value + "\">";
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("\"", "&quot;");
    }
}
