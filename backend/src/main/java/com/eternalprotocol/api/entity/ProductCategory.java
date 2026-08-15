package com.eternalprotocol.api.entity;

/**
 * The three top-level storefront sections — drives the MEN'S / WOMEN'S /
 * ACCESSORIES navbar structure directly, so this set is fixed (unlike
 * {@link Product#getSubCategory()}, which is free text set by the admin,
 * e.g. "Hoodies", "Caps" — those vary and grow over time, categories don't).
 */
public enum ProductCategory {
    MENS,
    WOMENS,
    ACCESSORIES
}
