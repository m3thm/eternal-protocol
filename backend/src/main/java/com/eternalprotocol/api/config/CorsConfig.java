package com.eternalprotocol.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configures CORS (Cross-Origin Resource Sharing) — the browser security
 * rule that normally blocks a website from calling an API hosted on a
 * different domain. Since the frontend (e.g. running on
 * {@code localhost:5173}) and this backend (e.g. {@code localhost:8080})
 * are different origins, this class explicitly allows the frontend to call
 * this API.
 */
@Configuration
public class CorsConfig {

    /** Comma-separated list of allowed frontend URLs, e.g. "http://localhost:5173,https://eternalprotocol.vercel.app". */
    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    /**
     * Builds the CORS rules applied to every request.
     *
     * @return the configured CORS rules
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
