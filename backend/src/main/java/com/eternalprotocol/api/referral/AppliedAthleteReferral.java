package com.eternalprotocol.api.referral;

import com.eternalprotocol.api.entity.Athlete;
import com.eternalprotocol.api.entity.Commission;
import com.eternalprotocol.api.entity.Order;
import com.eternalprotocol.api.repository.CommissionRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * A checkout that genuinely used a valid, active athlete code.
 * <p>
 * See {@link AthleteReferral} for why this exists alongside
 * {@link NoAthleteReferral} rather than a nullable {@code Athlete}.
 */
public final class AppliedAthleteReferral implements AthleteReferral {

    private final Athlete athlete;

    /**
     * @param athlete the athlete whose code was used at checkout, already
     *                confirmed to exist and be active by the caller
     */
    public AppliedAthleteReferral(Athlete athlete) {
        this.athlete = athlete;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Computed from the athlete's {@code commissionRate} — the same
     * percentage doubles as both the customer's discount and the
     * athlete's commission, per {@link Athlete#getCommissionRate()}.
     */
    @Override
    public BigDecimal calculateDiscount(BigDecimal subtotal) {
        return subtotal.multiply(athlete.getCommissionRate())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    @Override
    public String athleteCodeForOrder() {
        return athlete.getAthleteCode();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Always saves a new {@link Commission}, since a real athlete referral
     * always earns one.
     */
    @Override
    public void recordCommission(CommissionRepository commissionRepository, Order order, BigDecimal discountAmount) {
        commissionRepository.save(new Commission(athlete, order, discountAmount));
    }
}
