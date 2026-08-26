package com.eternalprotocol.api.factory.product;

import com.eternalprotocol.api.entity.ProductCategory;
import org.springframework.stereotype.Component;

/**
 * Picks the right {@link ProductCreator} subclass for a product category,
 * so {@code ProductService} never needs its own
 * {@code if (category == ...)}. The actual Factory Method logic lives one
 * level down, in {@link ProductCreator#createProduct}.
 */
@Component
public class ProductCreatorFactory {

    private final ApparelProductCreator apparelProductCreator = new ApparelProductCreator();
    private final AccessoryProductCreator accessoryProductCreator = new AccessoryProductCreator();

    /**
     * @param category the product's top-level category
     * @return the creator that applies the right size rule for that category
     */
    public ProductCreator creatorFor(ProductCategory category) {
        return switch (category) {
            case MENS, WOMENS -> apparelProductCreator;
            case ACCESSORIES -> accessoryProductCreator;
        };
    }
}
