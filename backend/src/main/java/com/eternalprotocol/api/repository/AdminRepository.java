package com.eternalprotocol.api.repository;

import com.eternalprotocol.api.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Data access for {@link Admin} rows.
 * <p>
 * A "repository" in Spring Data JPA is an interface with no implementation
 * needed — Spring generates the database code automatically at startup,
 * based on the method names below and the {@link JpaRepository} it extends
 * (which already provides basics like save, findById, findAll, delete).
 */
public interface AdminRepository extends JpaRepository<Admin, Long> {

    /**
     * Looks up an admin by login email.
     *
     * @param email email to search for
     * @return the matching admin, or empty if none found
     */
    Optional<Admin> findByEmail(String email);
}
