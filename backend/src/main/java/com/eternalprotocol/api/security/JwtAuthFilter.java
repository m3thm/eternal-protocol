package com.eternalprotocol.api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Checks every incoming request for a login token, before it reaches any
 * controller.
 * <p>
 * A "filter" in Spring runs on every HTTP request, ahead of the actual
 * endpoint code. This one looks for an {@code Authorization: Bearer <token>}
 * header. If a valid token is found, it resolves the user via
 * {@link CustomUserDetailsService} and stores them in Spring Security's
 * {@code SecurityContext}, which is what lets role checks elsewhere in the
 * app (like {@code @PreAuthorize} and {@code SecurityConfig}) know who is
 * making the request.
 * <p>
 * If there's no header, or it's invalid or expired, this filter does
 * nothing and simply lets the request continue as unauthenticated —
 * {@code SecurityConfig} is what actually blocks unauthenticated access to
 * protected endpoints with a 401/403 response.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    /**
     * @param jwtUtil            helper for reading and validating tokens
     * @param userDetailsService looks up the user a token's email belongs to
     */
    public JwtAuthFilter(JwtUtil jwtUtil, CustomUserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    /**
     * Runs once per incoming request. Reads and validates any bearer
     * token present, and if valid, marks the request as authenticated
     * before passing it along the filter chain.
     *
     * @param request     the incoming HTTP request
     * @param response    the outgoing HTTP response
     * @param filterChain the rest of the request-processing pipeline
     */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            String email = jwtUtil.extractEmail(token);

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                if (jwtUtil.isTokenValid(token, userDetails.getUsername())) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception ex) {
            // Malformed or expired token, or the user no longer exists —
            // treat the request as unauthenticated instead of failing with
            // a server error.
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
