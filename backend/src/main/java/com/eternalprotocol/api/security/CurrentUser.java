package com.eternalprotocol.api.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Helper for finding out "who is making this request" inside controllers
 * and services.
 * <p>
 * Controllers should always get the current user's ID from here — never
 * from a path variable or request body — because this reads it from the
 * verified JWT token, not from something the caller typed into the request.
 * This is what stops one customer or athlete from viewing another's data
 * just by changing an ID in the URL.
 */
@Component
public class CurrentUser {

    /**
     * Gets the full details of the currently authenticated user.
     *
     * @return the logged-in user's details
     * @throws IllegalStateException if called when nobody is authenticated
     */
    public AppUserDetails get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AppUserDetails userDetails)) {
            throw new IllegalStateException("No authenticated user in context");
        }
        return userDetails;
    }

    /**
     * Shortcut for the currently authenticated user's database ID.
     *
     * @return the logged-in user's ID
     */
    public Long id() {
        return get().getId();
    }
}
