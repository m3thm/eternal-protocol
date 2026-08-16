package com.eternalprotocol.api.service;

import com.eternalprotocol.api.dto.AuthResponseDto;
import com.eternalprotocol.api.dto.LoginRequestDto;
import com.eternalprotocol.api.dto.RegisterRequestDto;
import com.eternalprotocol.api.entity.Customer;
import com.eternalprotocol.api.exception.EmailAlreadyInUseException;
import com.eternalprotocol.api.exception.InvalidCredentialsException;
import com.eternalprotocol.api.repository.CustomerRepository;
import com.eternalprotocol.api.security.AppUserDetails;
import com.eternalprotocol.api.security.CustomUserDetailsService;
import com.eternalprotocol.api.security.JwtUtil;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Business logic for logging in and registering.
 * <p>
 * A "service" in Spring holds the actual business logic for a feature,
 * kept separate from controllers (which just handle HTTP) and repositories
 * (which just handle database access). Controllers call into services to
 * get work done.
 */
@Service
public class AuthService {

    private final CustomUserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final CustomerRepository customerRepository;

    /**
     * @param userDetailsService looks up a user by email across all three account tables
     * @param passwordEncoder    checks/hashes passwords
     * @param jwtUtil             issues login tokens
     * @param customerRepository  saves new customer accounts
     */
    public AuthService(CustomUserDetailsService userDetailsService,
                        PasswordEncoder passwordEncoder,
                        JwtUtil jwtUtil,
                        CustomerRepository customerRepository) {
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.customerRepository = customerRepository;
    }

    /**
     * Checks a login attempt against whichever account table matches the
     * email — customer, athlete, or admin — and issues a token if it's
     * correct.
     *
     * @param request email and password submitted
     * @return a login token plus the account's role and ID
     * @throws InvalidCredentialsException if the email/password is wrong, or the account is deactivated
     */
    public AuthResponseDto login(LoginRequestDto request) {
        AppUserDetails user;
        try {
            user = (AppUserDetails) userDetailsService.loadUserByUsername(request.email());
        } catch (UsernameNotFoundException ex) {
            throw new InvalidCredentialsException("Incorrect email or password");
        }

        if (!user.isEnabled()) {
            throw new InvalidCredentialsException("This account has been deactivated");
        }

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException("Incorrect email or password");
        }

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole(), user.getId());
        return new AuthResponseDto(token, user.getRole(), user.getId());
    }

    /**
     * Creates a new customer account and immediately logs them in.
     *
     * @param request new account's details
     * @return a login token for the newly created account
     * @throws EmailAlreadyInUseException if the email is already registered
     */
    public AuthResponseDto register(RegisterRequestDto request) {
        if (customerRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyInUseException("An account with this email already exists");
        }

        Customer customer = new Customer(request.name(), request.phone(), request.address());
        customer.setEmail(request.email());
        customer.setPasswordHash(passwordEncoder.encode(request.password()));
        customer = customerRepository.save(customer);

        String token = jwtUtil.generateToken(customer.getEmail(), "CUSTOMER", customer.getId());
        return new AuthResponseDto(token, "CUSTOMER", customer.getId());
    }
}
