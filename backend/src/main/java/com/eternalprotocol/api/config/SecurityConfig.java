package com.eternalprotocol.api.config;

import com.eternalprotocol.api.security.CustomUserDetailsService;
import com.eternalprotocol.api.security.JwtAuthFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Central place defining which API routes are public and which require a
 * CUSTOMER, ATHLETE, or ADMIN login.
 * <p>
 * This app is "stateless": it never uses server-side sessions or cookies to
 * remember who's logged in. Instead, every request proves who it's from by
 * sending a JWT token (see {@link JwtAuthFilter} and
 * {@code com.eternalprotocol.api.security.JwtUtil}), which scales cleanly
 * across the three different user types without any shared session state.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final CustomUserDetailsService userDetailsService;
    private final CorsConfigurationSource corsConfigurationSource;
    private final ObjectMapper objectMapper;

    /**
     * @param jwtAuthFilter           filter that reads and validates login tokens on every request
     * @param userDetailsService      looks up users by email during login
     * @param corsConfigurationSource allowed-origins rules from {@link CorsConfig}
     * @param objectMapper            used to write JSON error bodies for auth failures
     */
    public SecurityConfig(JwtAuthFilter jwtAuthFilter,
                           CustomUserDetailsService userDetailsService,
                           CorsConfigurationSource corsConfigurationSource,
                           ObjectMapper objectMapper) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.userDetailsService = userDetailsService;
        this.corsConfigurationSource = corsConfigurationSource;
        this.objectMapper = objectMapper;
    }

    /**
     * The password hashing algorithm used everywhere passwords are stored
     * or checked. BCrypt is a standard, secure choice for password hashing.
     *
     * @return the shared password encoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Tells Spring Security how to verify a login attempt: look the user
     * up via {@link CustomUserDetailsService}, then check their password
     * with {@link #passwordEncoder()}.
     *
     * @return the configured authentication provider
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * Exposes Spring Security's {@link AuthenticationManager}, used by the
     * login endpoint to actually attempt authenticating a submitted
     * email/password pair.
     *
     * @param config Spring Security's authentication setup
     * @return the shared authentication manager
     * @throws Exception if Spring Security fails to build the manager
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Defines the full set of access rules for every API route: which
     * endpoints are public, and which require which role.
     *
     * @param http Spring Security's request-security builder
     * @return the finished security rules
     * @throws Exception if the security chain fails to build
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // stateless JWT API, no cookies to protect against CSRF for
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public: auth endpoints
                        .requestMatchers("/api/auth/**").permitAll()

                        // Public: browsing the storefront
                        .requestMatchers(HttpMethod.GET, "/api/products", "/api/products/**").permitAll()

                        // Public: checkout, PayHere redirect + webhook, order lookup for confirmation page
                        .requestMatchers(HttpMethod.POST, "/api/orders").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/orders/*/checkout-redirect").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/orders/*/confirm-payment").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/orders/*").permitAll()

                        // Public: athlete code validation (live discount preview)
                        .requestMatchers(HttpMethod.GET, "/api/athletes/validate-code/**").permitAll()

                        // Admin-only product management (create/update/delete/image upload)
                        .requestMatchers(HttpMethod.POST, "/api/products").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/products/upload-image").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/products/style/*/images").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/products/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasRole("ADMIN")

                        // Role-restricted
                        .requestMatchers("/api/athletes/**").hasRole("ATHLETE")
                        .requestMatchers("/api/customers/**").hasRole("CUSTOMER")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                // Explicitly split "not authenticated at all" (401) from
                // "authenticated but wrong role" (403). Spring Security's
                // fallback behavior here is version/config-dependent and a
                // well-known source of APIs that return 403 for a missing
                // token, which isn't correct REST semantics and isn't what
                // the frontend's error handling expects — so we pin both
                // explicitly rather than relying on defaults.
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) ->
                                writeJsonError(response, 401, "Unauthorized",
                                        "Authentication required — please log in."))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeJsonError(response, 403, "Forbidden",
                                        "You do not have permission to perform this action."))
                );

        return http.build();
    }

    /**
     * Writes a JSON error body directly to the response, used for
     * authentication/authorization failures that happen before a
     * controller (and {@code GlobalExceptionHandler}) ever gets involved.
     *
     * @param response the HTTP response to write to
     * @param status   HTTP status code to send
     * @param error    short error name
     * @param message  human-readable explanation
     * @throws java.io.IOException if writing the response fails
     */
    private void writeJsonError(jakarta.servlet.http.HttpServletResponse response, int status,
                                 String error, String message) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status);
        body.put("error", error);
        body.put("message", message);

        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
