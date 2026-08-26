package com.eternalprotocol.api.factory.product;

import com.eternalprotocol.api.dto.ProductRequestDto;
import com.eternalprotocol.api.entity.ProductCategory;

/**
 * Size rule for {@link ProductCategory#ACCESSORIES}: most accessories
 * don't come in graded sizes, so a blank size defaults to
 * {@value #DEFAULT_SIZE}. An explicit size, if given, is still respected.
 */
public class AccessoryProductCreator extends ProductCreator {

    /** Size stamped on an accessory variant when the admin didn't specify one. */
    static final String DEFAULT_SIZE = "One Size";

    @Override
    protected String resolveSize(ProductRequestDto request) {
        return (request.size() == null || request.size().isBlank()) ? DEFAULT_SIZE : request.size();
    }
}
