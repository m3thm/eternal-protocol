package com.eternalprotocol.api.entity;

/**
 * The stages a {@link Commission} moves through, in order.
 */
public enum CommissionStatus {
    /**
     * Order was placed using the athlete's code, but PayHere payment has
     * not been confirmed yet. Not the same as "earned but unpaid" — this
     * is an earlier stage where the order itself isn't confirmed paid.
     */
    AWAITING_PAYMENT,

    /** Payment confirmed. The athlete has earned this commission but has not been paid out yet. */
    EARNED,

    /** Admin has paid the athlete for this commission. Final stage. */
    PAID
}
