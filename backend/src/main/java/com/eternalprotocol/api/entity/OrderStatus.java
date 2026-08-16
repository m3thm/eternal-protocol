package com.eternalprotocol.api.entity;

/**
 * The possible payment states of an {@link Order}.
 */
public enum OrderStatus {
    /** Order created, but PayHere payment not yet confirmed. */
    PENDING,

    /** Payment confirmed by PayHere. */
    PAID,

    /** Order was cancelled before payment completed. */
    CANCELLED
}
