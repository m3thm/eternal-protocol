package com.eternalprotocol.api.factory.product;

import com.eternalprotocol.api.dto.ProductRequestDto;
import com.eternalprotocol.api.entity.Product;

/**
 * Factory Method for building a {@link Product} variant, where a
 * category-specific subclass decides one detail of the construction: how
 * the variant's {@code size} is resolved.
 * <p>
 * This is a deliberately adapted realisation of the pattern for a domain
 * where the "product" being created (in the GoF sense — a {@code Product}
 * database row) is a single JPA entity class with no subclasses of its
 * own, unlike the classic Pizza Store example (or this codebase's own
 * {@link com.eternalprotocol.api.factory.CustomerFactory}'s reference to
 * it) where the factory method picks between genuinely different concrete
 * classes such as {@code NYStyleCheesePizza} versus
 * {@code ChicagoStyleCheesePizza}. What varies here isn't the concrete
 * class returned — it's always a {@code Product} — but which
 * category-specific rule a subclass applies while building it. The
 * pattern's actual defining shape survives that adaptation intact: this
 * class defines {@link #createProduct} as a fixed template that a caller
 * invokes without knowing which subclass it's talking to, and defers one
 * specific decision — {@link #resolveSize} — to whichever subclass is
 * plugged in, exactly as "lets subclasses decide" describes.
 * <p>
 * Two concrete rules exist: {@link ApparelProductCreator} for MENS/WOMENS,
 * which requires an explicit size, and {@link AccessoryProductCreator} for
 * ACCESSORIES, which defaults to "One Size" when none is given —
 * see {@link com.eternalprotocol.api.factory.product.ProductCreatorFactory}
 * for how a category is mapped to the right one.
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
