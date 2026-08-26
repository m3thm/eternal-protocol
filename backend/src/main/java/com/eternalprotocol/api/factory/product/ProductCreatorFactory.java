package com.eternalprotocol.api.factory.product;

import com.eternalprotocol.api.entity.ProductCategory;
import org.springframework.stereotype.Component;

/**
 * Picks the right {@link ProductCreator} subclass for a product category.
 * <p>
 * This class is plumbing, not the pattern itself — it's the same kind of
 * small dispatch this codebase's {@link com.eternalprotocol.api.factory.CustomerFactory}
 * demonstrates as a Simple Factory. The actual Factory Method lives one
 * level down, inside {@link ProductCreator#createProduct}, which calls the
 * abstract {@link ProductCreator#resolveSize} that each returned subclass
 * implements differently. This class's only job is choosing which of
 * those subclasses a given category should use, so
 * {@code ProductService} never needs an {@code if (category == ...)}
 * of its own.
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
