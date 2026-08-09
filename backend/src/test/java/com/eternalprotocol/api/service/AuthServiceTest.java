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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private CustomUserDetailsService userDetailsService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private AuthService authService;

    @Test
    void login_validCustomerCredentials_returnsToken() {
        AppUserDetails user = new AppUserDetails(1L, "jane@example.com", "hashed", "CUSTOMER", true);
        LoginRequestDto request = new LoginRequestDto("jane@example.com", "plaintext");

        when(userDetailsService.loadUserByUsername("jane@example.com")).thenReturn(user);
        when(passwordEncoder.matches("plaintext", "hashed")).thenReturn(true);
        when(jwtUtil.generateToken("jane@example.com", "CUSTOMER", 1L)).thenReturn("fake-jwt-token");

        AuthResponseDto result = authService.login(request);

        assertThat(result.token()).isEqualTo("fake-jwt-token");
        assertThat(result.role()).isEqualTo("CUSTOMER");
        assertThat(result.userId()).isEqualTo(1L);
    }

    @Test
    void login_unknownEmail_throwsInvalidCredentials() {
        LoginRequestDto request = new LoginRequestDto("ghost@example.com", "whatever");
        when(userDetailsService.loadUserByUsername("ghost@example.com"))
                .thenThrow(new UsernameNotFoundException("not found"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_wrongPassword_throwsInvalidCredentials() {
        AppUserDetails user = new AppUserDetails(1L, "jane@example.com", "hashed", "CUSTOMER", true);
        LoginRequestDto request = new LoginRequestDto("jane@example.com", "wrongpassword");

        when(userDetailsService.loadUserByUsername("jane@example.com")).thenReturn(user);
        when(passwordEncoder.matches("wrongpassword", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(jwtUtil, never()).generateToken(any(), any(), any());
    }

    @Test
    void login_deactivatedAthlete_throwsInvalidCredentialsBeforeCheckingPassword() {
        AppUserDetails inactiveAthlete = new AppUserDetails(2L, "athlete@example.com", "hashed", "ATHLETE", false);
        LoginRequestDto request = new LoginRequestDto("athlete@example.com", "correctpassword");

        when(userDetailsService.loadUserByUsername("athlete@example.com")).thenReturn(inactiveAthlete);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("deactivated");
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void register_newEmail_createsCustomerAndReturnsToken() {
        RegisterRequestDto request = new RegisterRequestDto(
                "Jane Doe", "jane@example.com", "password123", "0771234567", "123 Main St");

        when(customerRepository.existsByEmail("jane@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed-password");
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> {
            Customer c = inv.getArgument(0);
            c.setId(5L);
            return c;
        });
        when(jwtUtil.generateToken(eq("jane@example.com"), eq("CUSTOMER"), eq(5L))).thenReturn("new-jwt");

        AuthResponseDto result = authService.register(request);

        assertThat(result.token()).isEqualTo("new-jwt");
        assertThat(result.role()).isEqualTo("CUSTOMER");
        assertThat(result.userId()).isEqualTo(5L);

        verify(customerRepository).save(argThat(c ->
                c.getEmail().equals("jane@example.com")
                        && c.getPasswordHash().equals("hashed-password")
                        && c.getName().equals("Jane Doe")));
    }

    @Test
    void register_emailAlreadyInUse_throwsAndNeverSaves() {
        RegisterRequestDto request = new RegisterRequestDto(
                "Jane Doe", "existing@example.com", "password123", "0771234567", "123 Main St");
        when(customerRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EmailAlreadyInUseException.class);
        verify(customerRepository, never()).save(any());
    }
}
