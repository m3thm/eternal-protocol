package com.eternalprotocol.api.factory.product;

import com.eternalprotocol.api.dto.ProductRequestDto;
import com.eternalprotocol.api.entity.ProductCategory;

/**
 * Size rule for the {@link ProductCategory#ACCESSORIES} category: most
 * accessories (caps, bags, and similar) don't come in graded sizes the way
 * clothing does, so an admin can leave the size field blank and this
 * creator fills in a standard {@value #DEFAULT_SIZE} instead of forcing a
 * meaningless value to be typed in every time. An admin who does have a
 * genuinely sized accessory can still type an explicit size, which is
 * respected as-is.
 */
public class AccessoryProductCreator extends ProductCreator {

    /** Size stamped on an accessory variant when the admin didn't specify one. */
    static final String DEFAULT_SIZE = "One Size";

    @Override
    protected String resolveSize(ProductRequestDto request) {
        return (request.size() == null || request.size().isBlank()) ? DEFAULT_SIZE : request.size();
    }
}
