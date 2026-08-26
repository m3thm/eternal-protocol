package com.eternalprotocol.api.factory.product;

import com.eternalprotocol.api.dto.ProductRequestDto;
import com.eternalprotocol.api.entity.ProductCategory;

/**
 * Size rule for the {@link ProductCategory#MENS} and
 * {@link ProductCategory#WOMENS} categories: clothing genuinely comes in
 * different sizes, so an admin must specify one explicitly — there's no
 * sensible default to fall back on.
 */
public class ApparelProductCreator extends ProductCreator {

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if no size was given
     */
    @Override
    protected String resolveSize(ProductRequestDto request) {
        if (request.size() == null || request.size().isBlank()) {
            throw new IllegalArgumentException("Apparel products must specify a size");
        }
        return request.size();
    }
}
