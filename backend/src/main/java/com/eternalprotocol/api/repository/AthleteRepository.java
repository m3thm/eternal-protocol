package com.eternalprotocol.api.repository;

import com.eternalprotocol.api.entity.Athlete;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Data access for {@link Athlete} rows. Spring generates the implementation
 * of every method below automatically from its name.
 */
public interface AthleteRepository extends JpaRepository<Athlete, Long> {

    /**
     * Looks up an athlete by login email.
     *
     * @param email email to search for
     * @return the matching athlete, or empty if none found
     */
    Optional<Athlete> findByEmail(String email);

    /**
     * Looks up an athlete by their discount/referral code, ignoring
     * upper/lower case differences.
     *
     * @param athleteCode code to search for
     * @return the matching athlete, or empty if none found
     */
    Optional<Athlete> findByAthleteCodeIgnoreCase(String athleteCode);

    /**
     * Checks whether an email is already taken by an athlete account.
     *
     * @param email email to check
     * @return true if an athlete already uses this email
     */
    boolean existsByEmail(String email);

    /**
     * Checks whether a code is already taken, ignoring upper/lower case.
     *
     * @param athleteCode code to check
     * @return true if this code is already in use
     */
    boolean existsByAthleteCodeIgnoreCase(String athleteCode);
}
