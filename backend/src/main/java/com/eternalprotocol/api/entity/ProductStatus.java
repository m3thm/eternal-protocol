package com.eternalprotocol.api.entity;

/**
 * Where a product variant sits in its storefront lifecycle.
 * <p>
 * Deliberately independent of stock — a product can be {@code ACTIVE} with
 * zero stock (shown as "sold out", see {@code Product#getStockQty()}), and
 * a {@code COMING_SOON} product's stock is irrelevant until it launches.
 * <ul>
 *   <li>{@link #ACTIVE} — normal, purchasable product. Shown on its
 *       category page and, if recently added, the New Drops page.</li>
 *   <li>{@link #COMING_SOON} — visible only on the dedicated Upcoming
 *       page, never in the regular MEN'S/WOMEN'S/ACCESSORIES listings or
 *       New Drops. Can never be added to a cart or ordered, regardless of
 *       stock — enforced server-side in {@code CartService} and
 *       {@code OrderService}, not just hidden in the UI.</li>
 * </ul>
 * An admin flips a product from {@code COMING_SOON} to {@code ACTIVE} to
 * "launch" it — its category, subcategory, and other fields carry over
 * unchanged.
 */
public enum ProductStatus {
    ACTIVE,
    COMING_SOON
}
