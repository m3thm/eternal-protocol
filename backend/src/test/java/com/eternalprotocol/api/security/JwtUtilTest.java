package com.eternalprotocol.api.security;

import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pure unit tests — no Spring context needed, JwtUtil only depends on
 * plain values, so we construct it directly with test constants.
 */
class JwtUtilTest {

    private static final String TEST_SECRET = "test-secret-key-that-is-at-least-256-bits-long-for-hs256";
    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(TEST_SECRET, 86_400_000L); // 24h expiry
    }

    @Test
    void generatedToken_carriesEmailRoleAndUserId() {
        String token = jwtUtil.generateToken("athlete@example.com", "ATHLETE", 42L);

        assertThat(jwtUtil.extractEmail(token)).isEqualTo("athlete@example.com");
        assertThat(jwtUtil.extractRole(token)).isEqualTo("ATHLETE");
        assertThat(jwtUtil.extractUserId(token)).isEqualTo(42L);
    }

    @Test
    void isTokenValid_trueForMatchingEmailAndUnexpiredToken() {
        String token = jwtUtil.generateToken("customer@example.com", "CUSTOMER", 1L);

        assertThat(jwtUtil.isTokenValid(token, "customer@example.com")).isTrue();
    }

    @Test
    void isTokenValid_falseWhenEmailDoesNotMatch() {
        String token = jwtUtil.generateToken("customer@example.com", "CUSTOMER", 1L);

        assertThat(jwtUtil.isTokenValid(token, "someone-else@example.com")).isFalse();
    }

    @Test
    void isTokenValid_falseForExpiredToken() throws InterruptedException {
        JwtUtil shortLivedJwt = new JwtUtil(TEST_SECRET, 1L); // 1ms expiry
        String token = shortLivedJwt.generateToken("admin@example.com", "ADMIN", 1L);

        Thread.sleep(10); // let it expire

        assertThatThrownBy(() -> shortLivedJwt.isTokenValid(token, "admin@example.com"))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void tokensSignedWithDifferentSecrets_areRejected() {
        String token = jwtUtil.generateToken("customer@example.com", "CUSTOMER", 1L);
        JwtUtil differentSecretJwt = new JwtUtil(
                "a-completely-different-secret-key-thats-also-256-bits-long!!", 86_400_000L);

        assertThatThrownBy(() -> differentSecretJwt.extractEmail(token))
                .isInstanceOf(io.jsonwebtoken.security.SignatureException.class);
    }
}
