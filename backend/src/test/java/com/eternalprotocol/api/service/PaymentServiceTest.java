package com.eternalprotocol.api.service;

import com.eternalprotocol.api.entity.Customer;
import com.eternalprotocol.api.entity.Order;
import com.eternalprotocol.api.entity.OrderItem;
import com.eternalprotocol.api.entity.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * No Spring context needed — PaymentService only depends on plain
 * @Value-injected strings, so we construct it directly with test constants
 * matching the @Value parameter order in the real constructor.
 */
class PaymentServiceTest {

    private static final String MERCHANT_ID = "1211149";
    private static final String MERCHANT_SECRET = "test-merchant-secret";
    private static final String CHECKOUT_URL = "https://sandbox.payhere.lk/pay/checkout";
    private static final String FRONTEND_BASE_URL = "http://localhost:5173";
    private static final String APP_BASE_URL = "http://localhost:8080";

    private PaymentService paymentService;
    private Order order;
    private Product tee;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(
                MERCHANT_ID, MERCHANT_SECRET, CHECKOUT_URL, FRONTEND_BASE_URL, APP_BASE_URL);

        Customer customer = new Customer("Jane Doe", "0771234567", "123 Main St");
        tee = new Product();
        tee.setId(1L);
        tee.setName("Protocol 001 Oversized Tee");
        tee.setPrice(new BigDecimal("4500.00"));

        order = new Order(customer, new BigDecimal("4500.00"), BigDecimal.ZERO, null);
        order.setId(100L);
        order.addItem(new OrderItem(order, tee, "M", "Black", 1, new BigDecimal("4500.00")));
    }

    @Test
    void initiatePayment_returnsOwnCheckoutRedirectUrl_notPayHereDirectly() {
        // We never send the browser straight to PayHere with a GET — see
        // the class-level comment in PaymentService for why (form POST +
        // secret hash required). This should always point back at our own
        // backend first.
        String url = paymentService.initiatePayment(order, order.getTotalAmount());

        assertThat(url).isEqualTo("http://localhost:8080/api/orders/100/checkout-redirect");
    }

    @Test
    void buildCheckoutFormHtml_containsCorrectMerchantAndOrderFields() {
        String html = paymentService.buildCheckoutFormHtml(order);

        assertThat(html).contains("name=\"merchant_id\" value=\"1211149\"");
        assertThat(html).contains("name=\"order_id\" value=\"100\"");
        assertThat(html).contains("name=\"amount\" value=\"4500.00\"");
        assertThat(html).contains("name=\"currency\" value=\"LKR\"");
        assertThat(html).contains("name=\"items\" value=\"Protocol 001 Oversized Tee x1\"");
        assertThat(html).contains("action=\"" + CHECKOUT_URL + "\"");
    }

    @Test
    void buildCheckoutFormHtml_multipleItems_joinsThemAllIntoOneItemsSummary() {
        Product hoodie = new Product();
        hoodie.setId(2L);
        hoodie.setName("Protocol 002 Hoodie");
        hoodie.setPrice(new BigDecimal("8000.00"));
        order.addItem(new OrderItem(order, hoodie, "L", "Grey", 2, new BigDecimal("8000.00")));

        String html = paymentService.buildCheckoutFormHtml(order);

        // One order, one payment, one "items" string covering everything —
        // not three separate PayHere payments for a 3-line cart.
        assertThat(html).contains(
                "name=\"items\" value=\"Protocol 001 Oversized Tee x1, Protocol 002 Hoodie x2\"");
    }

    @Test
    void buildCheckoutFormHtml_notifyUrlIsPerOrder_notAFixedConfigValue() {
        String html = paymentService.buildCheckoutFormHtml(order);

        assertThat(html).contains(
                "name=\"notify_url\" value=\"http://localhost:8080/api/orders/100/confirm-payment\"");
    }

    @Test
    void buildCheckoutFormHtml_returnAndCancelUrls_pointToSameOrderConfirmationRouteWithStatusHint() {
        String html = paymentService.buildCheckoutFormHtml(order);

        // Both share one frontend route (/order/{id}/confirmation) — the
        // `status` query param is the only thing that differs, and it's a
        // UX hint only (see class-level comment); the frontend must still
        // fetch GET /api/orders/{id} for the real payment status.
        assertThat(html).contains(
                "name=\"return_url\" value=\"http://localhost:5173/order/100/confirmation?status=success\"");
        assertThat(html).contains(
                "name=\"cancel_url\" value=\"http://localhost:5173/order/100/confirmation?status=cancelled\"");
    }

    @Test
    void buildCheckoutFormHtml_splitsFullNameIntoFirstAndLastName() {
        String html = paymentService.buildCheckoutFormHtml(order);

        assertThat(html).contains("name=\"first_name\" value=\"Jane\"");
        assertThat(html).contains("name=\"last_name\" value=\"Doe\"");
    }

    @Test
    void buildCheckoutFormHtml_singleWordName_usesDashAsLastName() {
        Customer soleNameCustomer = new Customer("Madonna", "0771234567", "123 Main St");
        Product product = new Product();
        product.setId(3L);
        product.setName("Tee");
        product.setPrice(new BigDecimal("1000.00"));
        Order soleNameOrder = new Order(soleNameCustomer, new BigDecimal("1000.00"), BigDecimal.ZERO, null);
        soleNameOrder.setId(200L);
        soleNameOrder.addItem(new OrderItem(soleNameOrder, product, "M", "Black", 1, new BigDecimal("1000.00")));

        String html = paymentService.buildCheckoutFormHtml(soleNameOrder);

        assertThat(html).contains("name=\"first_name\" value=\"Madonna\"");
        assertThat(html).contains("name=\"last_name\" value=\"-\"");
    }

    @Test
    void buildCheckoutFormHtml_includesValidHashMatchingPayHeresFormula() {
        String html = paymentService.buildCheckoutFormHtml(order);
        String expectedHash = computeExpectedHash("100", "4500.00", "LKR");

        assertThat(html).contains("name=\"hash\" value=\"" + expectedHash + "\"");
    }

    @Test
    void verifyNotifySignature_matchingSignature_returnsTrue() {
        String orderId = "100";
        String amount = "4500.00";
        String currency = "LKR";
        String statusCode = "2";
        String validSignature = computeExpectedSignature(orderId, amount, currency, statusCode);

        boolean result = paymentService.verifyNotifySignature(
                MERCHANT_ID, orderId, amount, currency, statusCode, validSignature);

        assertThat(result).isTrue();
    }

    @Test
    void verifyNotifySignature_tamperedAmount_returnsFalse() {
        // Signature was computed for 4500.00 but the webhook claims 1.00 —
        // this is exactly the kind of forged "payment succeeded" call the
        // signature check exists to catch.
        String validSignature = computeExpectedSignature("100", "4500.00", "LKR", "2");

        boolean result = paymentService.verifyNotifySignature(
                MERCHANT_ID, "100", "1.00", "LKR", "2", validSignature);

        assertThat(result).isFalse();
    }

    @Test
    void verifyNotifySignature_wrongSignatureEntirely_returnsFalse() {
        boolean result = paymentService.verifyNotifySignature(
                MERCHANT_ID, "100", "4500.00", "LKR", "2", "NOT-A-REAL-SIGNATURE");

        assertThat(result).isFalse();
    }

    // ---- test helpers: reimplement PayHere's published hash formula independently ----
    // (deliberately not calling any PaymentService internals, so this test
    // would actually catch a regression in the real formula)

    private String computeExpectedHash(String orderId, String amount, String currency) {
        String merchantSecretHash = md5Upper(MERCHANT_SECRET);
        return md5Upper(MERCHANT_ID + orderId + amount + currency + merchantSecretHash);
    }

    private String computeExpectedSignature(String orderId, String amount, String currency, String statusCode) {
        String merchantSecretHash = md5Upper(MERCHANT_SECRET);
        return md5Upper(MERCHANT_ID + orderId + amount + currency + statusCode + merchantSecretHash);
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
            throw new IllegalStateException(e);
        }
    }
}
