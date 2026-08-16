package com.eternalprotocol.api.service;

import com.eternalprotocol.api.entity.Order;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.DecimalFormat;

/**
 * Wraps the integration with PayHere, the payment gateway used for
 * checkout.
 * <p>
 * PayHere doesn't accept a simple link with payment details in the URL —
 * it requires an HTML form to be POSTed to its checkout page, including a
 * security hash computed from a secret key that must never reach the
 * customer's browser. So the "payment URL" this service hands back to the
 * frontend actually points to this app's own backend
 * ({@code GET /api/orders/{id}/checkout-redirect}), which serves a small
 * page that automatically submits the real PayHere form — keeping the
 * secret key safely on the server the whole time.
 * <p>
 * See PayHere's docs: https://support.payhere.lk/api-&-mobile-sdk/payhere-checkout
 */
@Service
public class PaymentService {

    private final String merchantId;
    private final String merchantSecret;
    private final String checkoutUrl;
    private final String frontendBaseUrl;
    private final String appBaseUrl;

    /**
     * @param merchantId      PayHere merchant account ID
     * @param merchantSecret  PayHere secret key, used to compute security hashes, never exposed to the browser
     * @param checkoutUrl     PayHere's hosted checkout page URL
     * @param frontendBaseUrl this app's frontend URL, used to build PayHere's return/cancel URLs
     * @param appBaseUrl      this app's own backend URL, used to build the redirect and webhook URLs
     */
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

    /**
     * Builds the URL the frontend should send the customer to right after
     * an order is created. The actual PayHere form fields (including the
     * security hash) are only built once that URL is visited, via
     * {@link #buildCheckoutFormHtml}, so nothing sensitive needs to be
     * stored ahead of time.
     *
     * @param order  the newly created order
     * @param amount amount to be charged
     * @return this app's own redirect URL for that order
     */
    public String initiatePayment(Order order, BigDecimal amount) {
        return appBaseUrl + "/api/orders/" + order.getId() + "/checkout-redirect";
    }

    /**
     * Builds the small self-submitting HTML page that sends the customer's
     * browser into PayHere's actual checkout form.
     * <p>
     * {@code return_url} and {@code cancel_url} both point at the same
     * frontend confirmation page, distinguished only by a
     * {@code status=success}/{@code status=cancelled} query parameter.
     * That parameter is a UX hint only — a customer can land on this page
     * without having actually paid (e.g. by typing the URL directly, or if
     * the payment webhook is delayed). The confirmation page must always
     * separately call {@code GET /api/orders/{id}} and check its real
     * {@code paymentStatus} field, which only ever becomes {@code PAID}
     * through the signature-verified webhook in
     * {@code OrderService.confirmPayment}.
     *
     * @param order the order being paid for
     * @return a complete HTML page that auto-submits to PayHere
     */
    public String buildCheckoutFormHtml(Order order) {
        String orderId = String.valueOf(order.getId());
        String amount = formatAmount(order.getTotalAmount());
        String currency = "LKR";
        String hash = computeHash(orderId, amount, currency);

        String[] nameParts = splitName(order.getCustomer().getName());

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
     * Verifies the md5 signature PayHere sends on its server-to-server
     * payment webhook. This must always pass before a "payment succeeded"
     * claim is trusted — without it, anyone could send a fake success
     * request directly to the webhook endpoint.
     *
     * @param merchantId      merchant ID from the webhook payload
     * @param orderId         order reference from the webhook payload
     * @param payhereAmount   amount PayHere recorded as paid
     * @param payhereCurrency currency PayHere recorded
     * @param statusCode      PayHere's payment result code
     * @param md5sig          the signature PayHere sent, to check against a locally computed one
     * @return true if the signature is genuine
     */
    public boolean verifyNotifySignature(String merchantId, String orderId, String payhereAmount,
                                          String payhereCurrency, String statusCode, String md5sig) {
        String merchantSecretHash = md5Upper(merchantSecret);
        String localSig = md5Upper(merchantId + orderId + payhereAmount + payhereCurrency
                + statusCode + merchantSecretHash);
        return localSig.equalsIgnoreCase(md5sig);
    }

    /**
     * Computes the security hash PayHere requires on the checkout form,
     * proving the request genuinely came from this backend and wasn't
     * tampered with in the browser.
     *
     * @param orderId  order reference
     * @param amount   amount formatted as PayHere expects
     * @param currency currency code
     * @return the computed hash
     */
    private String computeHash(String orderId, String amount, String currency) {
        String merchantSecretHash = md5Upper(merchantSecret);
        return md5Upper(merchantId + orderId + amount + currency + merchantSecretHash);
    }

    /**
     * Computes an uppercase MD5 hash of a string, the exact format
     * PayHere's hash checks require.
     *
     * @param input text to hash
     * @return the uppercase hex-encoded MD5 hash
     */
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

    /**
     * Formats an amount the way PayHere expects: always exactly two
     * decimal places.
     *
     * @param amount amount to format
     * @return the formatted amount string
     */
    private String formatAmount(BigDecimal amount) {
        return new DecimalFormat("0.00").format(amount);
    }

    /**
     * Builds a short human-readable summary of the order's contents for
     * PayHere's {@code items} field, which is just a display string shown
     * on PayHere's own checkout page — no structured format is required.
     * Capped at 200 characters, since very long text is impractical on a
     * payment page even though PayHere doesn't document a hard limit.
     *
     * @param order the order to summarize
     * @return a short summary like "Product Name x2, Other Product x1"
     */
    private String buildItemsSummary(Order order) {
        String summary = order.getItems().stream()
                .map(item -> item.getProduct().getName() + " x" + item.getQuantity())
                .collect(java.util.stream.Collectors.joining(", "));
        return summary.length() > 200 ? summary.substring(0, 197) + "..." : summary;
    }

    /**
     * Splits a full name into first and last name for PayHere's separate
     * {@code first_name}/{@code last_name} fields.
     *
     * @param fullName the full name to split
     * @return a two-element array: [firstName, lastName]
     */
    private String[] splitName(String fullName) {
        String[] parts = fullName.trim().split("\\s+", 2);
        return parts.length == 2 ? parts : new String[]{parts[0], "-"};
    }

    /**
     * Builds one hidden {@code <input>} field for the PayHere form.
     *
     * @param name  field name PayHere expects
     * @param value field value
     * @return the HTML for that hidden input
     */
    private String hidden(String name, String value) {
        return "<input type=\"hidden\" name=\"" + name + "\" value=\"" + value + "\">";
    }

    /**
     * Escapes a value for safe inclusion inside an HTML attribute.
     *
     * @param value text to escape, may be null
     * @return the escaped text, or an empty string if null
     */
    private String escape(String value) {
        return value == null ? "" : value.replace("\"", "&quot;");
    }
}
