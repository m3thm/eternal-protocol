package com.eternalprotocol.api.referral;

import com.eternalprotocol.api.entity.Commission;
import com.eternalprotocol.api.entity.Order;
import com.eternalprotocol.api.repository.CommissionRepository;

import java.math.BigDecimal;

/**
 * Whatever athlete referral — real or absent — applies to one checkout
 * (Null Object pattern).
 * <p>
 * Instead of a nullable {@code Athlete} and scattered null checks,
 * checkout always holds a real, non-null {@code AthleteReferral}: either
 * {@link AppliedAthleteReferral}, wrapping a genuine athlete code, or the
 * {@link NoAthleteReferral} singleton, which answers every question below
 * with "no effect" — including a no-op {@code recordCommission} — so
 * callers never need an {@code if (athlete != null)}.
 */
public interface AthleteReferral {

    /**
     * The discount this referral applies to an order's subtotal.
     *
     * @param subtotal the order's subtotal before any discount
     * @return the discount amount, or {@link BigDecimal#ZERO} for no referral
     */
    BigDecimal calculateDiscount(BigDecimal subtotal);

    /**
     * The athlete code that should be stamped onto the order, for display
     * on receipts and the admin order list.
     *
     * @return the athlete's code, or {@code null} for no referral
     */
    String athleteCodeForOrder();

    /**
     * Records a commission for this referral against a freshly-saved
     * order, if this referral is one that earns a commission at all.
     * <p>
     * {@link NoAthleteReferral} implements this as a complete no-op —
     * that's the point: {@code OrderService} can call this unconditionally
     * after saving every order, without first checking whether there was
     * an athlete.
     *
     * @param commissionRepository access to commission data
     * @param order                the order that was just saved
     * @param discountAmount       the discount that was applied, which becomes the commission amount
     */
    void recordCommission(CommissionRepository commissionRepository, Order order, BigDecimal discountAmount);
}
