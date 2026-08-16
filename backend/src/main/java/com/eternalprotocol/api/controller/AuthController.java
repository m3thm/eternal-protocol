package com.eternalprotocol.api.controller;

import com.eternalprotocol.api.dto.AuthResponseDto;
import com.eternalprotocol.api.dto.LoginRequestDto;
import com.eternalprotocol.api.dto.RegisterRequestDto;
import com.eternalprotocol.api.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST endpoints for logging in and registering.
 * <p>
 * A "controller" in Spring is the class that receives HTTP requests and
 * sends back responses. {@code @RestController} means every method's
 * return value is automatically converted to JSON. {@code @RequestMapping}
 * sets the shared URL prefix for every endpoint in this class. Controllers
 * stay thin here — the actual login/register logic lives in
 * {@link AuthService}.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    /**
     * @param authService handles the actual login/registration logic
     */
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Logs in a customer, athlete, or admin — all three share this one
     * endpoint, since the backend checks all three tables for the email.
     *
     * @param request email and password to check
     * @return a JWT token plus the account's role and ID
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * Registers a new customer account. Athletes and admins are created
     * separately by an admin, not through self-registration.
     *
     * @param request new account's details
     * @return a JWT token for the newly created account
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponseDto> register(@Valid @RequestBody RegisterRequestDto request) {
        return ResponseEntity.ok(authService.register(request));
    }
}
