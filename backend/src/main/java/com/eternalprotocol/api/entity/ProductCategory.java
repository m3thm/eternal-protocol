package com.eternalprotocol.api.entity;

/**
 * The three fixed top-level storefront sections. Drives the MEN'S /
 * WOMEN'S / ACCESSORIES navigation menu directly, so — unlike
 * {@link Product#getSubCategory()}, which is free text set by the admin —
 * this set of values never changes.
 */
public enum ProductCategory {
    MENS,
    WOMENS,
    ACCESSORIES
}
