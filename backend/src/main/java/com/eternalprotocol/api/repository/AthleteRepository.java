package com.eternalprotocol.api.repository;

import com.eternalprotocol.api.entity.Athlete;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AthleteRepository extends JpaRepository<Athlete, Long> {

    Optional<Athlete> findByEmail(String email);

    Optional<Athlete> findByAthleteCodeIgnoreCase(String athleteCode);

    boolean existsByEmail(String email);

    boolean existsByAthleteCodeIgnoreCase(String athleteCode);
}
