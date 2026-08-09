package com.eternalprotocol.api.repository;

import com.eternalprotocol.api.entity.Commission;
import com.eternalprotocol.api.entity.CommissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CommissionRepository extends JpaRepository<Commission, Long> {

    List<Commission> findByAthleteId(Long athleteId);

    List<Commission> findByCommissionStatus(CommissionStatus status);

    Optional<Commission> findByOrderId(Long orderId);
}
