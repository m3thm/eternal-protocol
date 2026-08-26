package com.eternalprotocol.api.referral;

import com.eternalprotocol.api.entity.Commission;
import com.eternalprotocol.api.entity.Order;
import com.eternalprotocol.api.repository.CommissionRepository;

import java.math.BigDecimal;

/**
 * Whatever athlete referral — real or absent — applies to one checkout.
 * <p>
 * Before this existed, {@code OrderService.createOrder} decided three
 * separate things by asking "is there an athlete?" every time: how big the
 * discount is, what code (if any) to stamp on the order, and whether a
 * {@link Commission} row should be saved at all. Three call sites, three
 * {@code athlete == null} checks, all needing to agree with each other.
 * <p>
 * This is exactly the shape the Null Object pattern targets. Instead of a
 * nullable {@code Athlete} plus scattered null checks, checkout always
 * holds a real, non-null {@code AthleteReferral} — either
 * {@link AppliedAthleteReferral}, wrapping a genuine athlete code, or the
 * {@link NoAthleteReferral} singleton, which answers every one of these
 * questions with "no effect" instead of the caller having to ask "is it
 * null?" first. {@link NoAthleteReferral#recordCommission} in particular
 * is the pattern's textbook case: a method that does nothing at all when
 * called, so the caller never needs an {@code if} around calling it.
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
