package com.eternalprotocol.api.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Represents the logged-in user to Spring Security, regardless of whether
 * they are a Customer, Athlete, or Admin in the database.
 * <p>
 * {@code UserDetails} is a Spring Security interface — implementing it is
 * what lets Spring Security's authentication/authorization machinery work
 * with this app's three different user tables without needing to know
 * which table a user actually came from. {@code role} is one of
 * {@code "CUSTOMER"}, {@code "ATHLETE"}, or {@code "ADMIN"}.
 */
public class AppUserDetails implements UserDetails {

    private final Long id;
    private final String email;
    private final String passwordHash;
    private final String role;
    private final boolean enabled;

    /**
     * @param id           user's database ID
     * @param email        login email
     * @param passwordHash bcrypt-hashed password
     * @param role         one of "CUSTOMER", "ATHLETE", "ADMIN"
     * @param enabled      whether this account is currently allowed to log in
     */
    public AppUserDetails(Long id, String email, String passwordHash, String role, boolean enabled) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.enabled = enabled;
    }

    /**
     * @return this user's database ID
     */
    public Long getId() {
        return id;
    }

    /**
     * @return this user's role: "CUSTOMER", "ATHLETE", or "ADMIN"
     */
    public String getRole() {
        return role;
    }

    /**
     * Spring Security permission list, built from the role. Roles are
     * prefixed with {@code "ROLE_"} because that's the convention Spring
     * Security's role-based checks (e.g. {@code hasRole("ADMIN")}) expect.
     *
     * @return this user's granted authorities
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    /**
     * @return the bcrypt-hashed password, used internally by Spring Security to check login attempts
     */
    @Override
    public String getPassword() {
        return passwordHash;
    }

    /**
     * @return the login email, used by Spring Security as this user's "username"
     */
    @Override
    public String getUsername() {
        return email;
    }

    /** @return true — account expiry isn't used in this app */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /** @return true — account locking isn't used in this app */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /** @return true — credential expiry isn't used in this app */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * @return whether this account can currently log in (e.g. false for a deactivated athlete)
     */
    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
