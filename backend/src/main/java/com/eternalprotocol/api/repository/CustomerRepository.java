package com.eternalprotocol.api.repository;

import com.eternalprotocol.api.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Data access for {@link Customer} rows. Spring generates the
 * implementation of every method below automatically from its name.
 */
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    /**
     * Looks up a customer by login email.
     *
     * @param email email to search for
     * @return the matching customer, or empty if none found
     */
    Optional<Customer> findByEmail(String email);

    /**
     * Checks whether an email is already taken by a customer account.
     *
     * @param email email to check
     * @return true if a customer already uses this email
     */
    boolean existsByEmail(String email);
}
