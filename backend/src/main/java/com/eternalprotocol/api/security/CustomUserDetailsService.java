package com.eternalprotocol.api.security;

import com.eternalprotocol.api.entity.Admin;
import com.eternalprotocol.api.entity.Athlete;
import com.eternalprotocol.api.entity.Customer;
import com.eternalprotocol.api.repository.AdminRepository;
import com.eternalprotocol.api.repository.AthleteRepository;
import com.eternalprotocol.api.repository.CustomerRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * The core of the "one login endpoint, three tables" decision: given an
 * email, look it up as a Customer, then Athlete, then Admin, and wrap
 * whichever one matches in a common AppUserDetails carrying the role.
 *
 * Guest customers (passwordHash == null) are deliberately skipped here —
 * they have no credentials to log in with, so they should never resolve to
 * a loadable user; a guest checkout customer trying to "log in" with their
 * email should just get "no account found", not a crash.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final CustomerRepository customerRepository;
    private final AthleteRepository athleteRepository;
    private final AdminRepository adminRepository;

    public CustomUserDetailsService(CustomerRepository customerRepository,
                                     AthleteRepository athleteRepository,
                                     AdminRepository adminRepository) {
        this.customerRepository = customerRepository;
        this.athleteRepository = athleteRepository;
        this.adminRepository = adminRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        return customerRepository.findByEmail(email)
                .filter(Customer::isRegistered)
                .map(c -> (UserDetails) new AppUserDetails(c.getId(), c.getEmail(), c.getPasswordHash(), "CUSTOMER", true))
                .or(() -> athleteRepository.findByEmail(email)
                        .map(a -> new AppUserDetails(a.getId(), a.getEmail(), a.getPasswordHash(), "ATHLETE", a.isActive())))
                .or(() -> adminRepository.findByEmail(email)
                        .map((Admin ad) -> new AppUserDetails(ad.getId(), ad.getEmail(), ad.getPasswordHash(), "ADMIN", true)))
                .orElseThrow(() -> new UsernameNotFoundException("No account for " + email));
    }
}
