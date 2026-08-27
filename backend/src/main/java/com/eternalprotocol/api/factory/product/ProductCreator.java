package com.eternalprotocol.api.factory.product;

import com.eternalprotocol.api.dto.ProductRequestDto;
import com.eternalprotocol.api.entity.Product;
import com.eternalprotocol.api.entity.ProductStatus;

/**
 * Factory Method for building a {@link Product} variant, where a
 * category-specific subclass decides one detail of construction: how the
 * variant's {@code size} is resolved.
 * <p>
 * {@link #createProduct} is a fixed template a caller invokes without
 * knowing which subclass it's talking to; only {@link #resolveSize} is
 * deferred to the plugged-in subclass. Two rules exist:
 * {@link ApparelProductCreator} (MENS/WOMENS, requires an explicit size)
 * and {@link AccessoryProductCreator} (ACCESSORIES, defaults to "One
 * Size") — see {@link ProductCreatorFactory} for how a category picks one.
 */
public abstract class ProductCreator {

    /**
     * Builds a brand-new, unsaved {@link Product} from a create request,
     * applying every common field plus this creator's category-specific
     * size rule.
     *
     * @param request the incoming create request
     * @return a new, unsaved {@code Product} ready to be persisted
     */
    public final Product createProduct(ProductRequestDto request) {
        Product product = new Product();
        applyCommonFields(product, request);
        return product;
    }

    /**
     * Copies a request's fields onto an existing product entity — shared by
     * both {@link #createProduct} (a brand-new entity) and
     * {@code ProductService.updateProduct} (an existing one), so an edit
     * applies exactly the same category size rule a fresh creation would.
     *
     * @param product the entity to update in place
     * @param request the incoming request data
     */
    public final void applyCommonFields(Product product, ProductRequestDto request) {
        product.setStyleCode(request.styleCode());
        product.setName(request.name());
        product.setCategory(request.category());
        product.setSubCategory(request.subCategory());
        product.setPrice(request.price());
        product.setColour(request.colour());
        product.setStockQty(request.stockQty());
        product.setDescription(request.description());
        product.setSize(resolveSize(request));
        // Both default when omitted, rather than requiring every caller
        // (including existing tests/scripts) to always send them.
        product.setStatus(request.status() != null ? request.status() : ProductStatus.ACTIVE);
        product.setShowPrice(request.showPrice() != null ? request.showPrice() : true);
    }

    /**
     * The factory method: decides what value this product variant's
     * {@code size} field should hold. This is the one construction
     * decision {@link #createProduct} and {@link #applyCommonFields} defer
     * to a subclass rather than deciding themselves.
     *
     * @param request the incoming create/update request
     * @return the size value to store on the product
     * @throws IllegalArgumentException if this category requires a size and none was given
     */
    protected abstract String resolveSize(ProductRequestDto request);
}
