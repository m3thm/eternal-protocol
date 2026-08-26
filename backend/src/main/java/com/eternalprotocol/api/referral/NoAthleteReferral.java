package com.eternalprotocol.api.referral;

import com.eternalprotocol.api.entity.Order;
import com.eternalprotocol.api.repository.CommissionRepository;

import java.math.BigDecimal;

/**
 * The Null Object case: no athlete code was used, or the one submitted
 * didn't match an active athlete.
 * <p>
 * Every method here answers with "no effect" — zero discount, no code, and
 * {@link #recordCommission} does nothing at all — instead of the caller
 * needing an {@code if (athlete != null)} around each of these decisions.
 * See {@link AthleteReferral} for the full rationale.
 * <p>
 * Stateless, so a single shared {@link #INSTANCE} is used everywhere
 * rather than constructing a new one per checkout.
 */
public final class NoAthleteReferral implements AthleteReferral {

    /** The single shared instance — this class has no state, so one is all that's ever needed. */
    public static final NoAthleteReferral INSTANCE = new NoAthleteReferral();

    private NoAthleteReferral() {
    }

    /** @return {@link BigDecimal#ZERO} — no athlete means no discount */
    @Override
    public BigDecimal calculateDiscount(BigDecimal subtotal) {
        return BigDecimal.ZERO;
    }

    /** @return {@code null} — no athlete code to stamp on the order */
    @Override
    public String athleteCodeForOrder() {
        return null;
    }

    /**
     * Does nothing. This is the pattern's central case: {@code OrderService}
     * calls this unconditionally after every order is saved, and for a
     * checkout with no athlete, that call simply has no effect.
     */
    @Override
    public void recordCommission(CommissionRepository commissionRepository, Order order, BigDecimal discountAmount) {
        // Intentionally empty — no athlete, no commission.
    }
}
