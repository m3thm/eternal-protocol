package com.eternalprotocol.api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

/**
 * Creates and reads JWTs (JSON Web Tokens) — the login tokens issued after
 * a successful sign-in, and checked on every later request.
 * <p>
 * A JWT is a signed, tamper-proof piece of text that encodes information
 * about who is logged in. This app embeds the user's email (as the
 * standard "subject") and a custom {@code role} claim ("CUSTOMER",
 * "ATHLETE", or "ADMIN") in every token, so one token format works for all
 * three account types.
 */
@Component
public class JwtUtil {

    private final SecretKey signingKey;
    private final long expirationMs;

    /**
     * @param secret       signing secret, must be at least 256 bits (32 characters) long for the HS256 algorithm used here
     * @param expirationMs how long a generated token stays valid, in milliseconds
     */
    public JwtUtil(@Value("${app.jwt.secret}") String secret,
                    @Value("${app.jwt.expiration-ms:86400000}") long expirationMs) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.expirationMs = expirationMs;
    }

    /**
     * Builds a new signed token for a just-logged-in user.
     *
     * @param email  user's login email, stored as the token's subject
     * @param role   "CUSTOMER", "ATHLETE", or "ADMIN"
     * @param userId user's database ID
     * @return the signed token string
     */
    public String generateToken(String email, String role, Long userId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .claim("userId", userId)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    /**
     * Reads the email stored in a token.
     *
     * @param token the token to read
     * @return the email it was issued for
     */
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Reads the role stored in a token.
     *
     * @param token the token to read
     * @return "CUSTOMER", "ATHLETE", or "ADMIN"
     */
    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    /**
     * Reads the user ID stored in a token.
     *
     * @param token the token to read
     * @return the user's database ID
     */
    public Long extractUserId(String token) {
        return extractAllClaims(token).get("userId", Long.class);
    }

    /**
     * Checks whether a token is genuine, unexpired, and issued for the
     * expected email.
     *
     * @param token         the token to check
     * @param expectedEmail the email the token should belong to
     * @return true if the token is valid for this email
     */
    public boolean isTokenValid(String token, String expectedEmail) {
        String email = extractEmail(token);
        return email.equals(expectedEmail) && !isTokenExpired(token);
    }

    /**
     * Checks whether a token's expiry time has already passed.
     *
     * @param token the token to check
     * @return true if the token has expired
     */
    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    /**
     * Reads one specific claim out of a token using a resolver function.
     *
     * @param token    the token to read
     * @param resolver function that picks out the desired claim
     * @param <T>      the claim's type
     * @return the resolved claim value
     */
    private <T> T extractClaim(String token, Function<Claims, T> resolver) {
        return resolver.apply(extractAllClaims(token));
    }

    /**
     * Verifies a token's signature and returns every claim it carries.
     * Throws if the token is malformed, expired, or signed with a
     * different key.
     *
     * @param token the token to verify and read
     * @return all claims stored in the token
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
