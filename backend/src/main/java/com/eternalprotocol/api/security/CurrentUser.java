package com.eternalprotocol.api.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Centralizes "who is making this request" so controllers never read a
 * user/athlete/admin ID from a path variable or request body — always from
 * the verified JWT via the SecurityContext. This is what stops one athlete
 * (or customer) from viewing another's data by editing a URL.
 */
@Component
public class CurrentUser {

    public AppUserDetails get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AppUserDetails userDetails)) {
            throw new IllegalStateException("No authenticated user in context");
        }
        return userDetails;
    }

    public Long id() {
        return get().getId();
    }
}
