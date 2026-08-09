package com.eternalprotocol.api.entity;

public enum CommissionStatus {
    /**
     * Order placed with this athlete's code but PayHere payment not yet confirmed.
     * Note: the client brief calls this stage "pending" too (as in "pending
     * commission"), which is easy to confuse with EARNED-but-unpaid. We use
     * AWAITING_PAYMENT here specifically so the two states in the brief
     * ("payment not yet confirmed" vs "earned but not yet paid out") aren't
     * both called PENDING in the code — worth mentioning in the report.
     */
    AWAITING_PAYMENT,
    /** Payment confirmed — commission earned but not yet paid out to the athlete. */
    EARNED,
    /** Admin has paid the athlete for this commission. */
    PAID
}
