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
 * Looks up a user by email across all three account types, for Spring
 * Security to use during login.
 * <p>
 * This app has a single login endpoint shared by customers, athletes, and
 * admins, even though they live in three separate database tables. Given
 * an email, this service checks the Customer table, then Athlete, then
 * Admin, and wraps whichever one matches in a common {@link AppUserDetails}
 * that carries the matched role.
 * <p>
 * Guest customers (customers with no password set) are deliberately
 * skipped — they have no login credentials, so trying to log in with a
 * guest checkout email should simply result in "no account found", not an
 * error.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final CustomerRepository customerRepository;
    private final AthleteRepository athleteRepository;
    private final AdminRepository adminRepository;

    /**
     * @param customerRepository access to customer accounts
     * @param athleteRepository  access to athlete accounts
     * @param adminRepository    access to admin accounts
     */
    public CustomUserDetailsService(CustomerRepository customerRepository,
                                     AthleteRepository athleteRepository,
                                     AdminRepository adminRepository) {
        this.customerRepository = customerRepository;
        this.athleteRepository = athleteRepository;
        this.adminRepository = adminRepository;
    }

    /**
     * Finds the account matching this email, checking customers first,
     * then athletes, then admins.
     *
     * @param email the login email to look up
     * @return the matched user, wrapped with their role
     * @throws UsernameNotFoundException if no account exists for this email
     */
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
