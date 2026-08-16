package com.eternalprotocol.api.repository;

import com.eternalprotocol.api.entity.Commission;
import com.eternalprotocol.api.entity.CommissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Data access for {@link Commission} rows. Spring generates the
 * implementation of every method below automatically from its name.
 */
public interface CommissionRepository extends JpaRepository<Commission, Long> {

    /**
     * Gets every commission earned by one athlete, across all their orders.
     *
     * @param athleteId athlete's ID
     * @return that athlete's commissions
     */
    List<Commission> findByAthleteId(Long athleteId);

    /**
     * Gets every commission currently in a given status, e.g. all
     * {@code EARNED} commissions still awaiting payout.
     *
     * @param status status to filter by
     * @return matching commissions
     */
    List<Commission> findByCommissionStatus(CommissionStatus status);

    /**
     * Looks up the single commission tied to one order, if any.
     *
     * @param orderId order's ID
     * @return the matching commission, or empty if that order has none
     */
    Optional<Commission> findByOrderId(Long orderId);
}
