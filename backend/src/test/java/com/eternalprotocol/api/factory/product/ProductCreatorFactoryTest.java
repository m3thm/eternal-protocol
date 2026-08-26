package com.eternalprotocol.api.factory.product;

import com.eternalprotocol.api.entity.ProductCategory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductCreatorFactoryTest {

    private final ProductCreatorFactory factory = new ProductCreatorFactory();

    @Test
    void creatorFor_mens_returnsApparelProductCreator() {
        assertThat(factory.creatorFor(ProductCategory.MENS)).isInstanceOf(ApparelProductCreator.class);
    }

    @Test
    void creatorFor_womens_returnsApparelProductCreator() {
        assertThat(factory.creatorFor(ProductCategory.WOMENS)).isInstanceOf(ApparelProductCreator.class);
    }

    @Test
    void creatorFor_accessories_returnsAccessoryProductCreator() {
        assertThat(factory.creatorFor(ProductCategory.ACCESSORIES)).isInstanceOf(AccessoryProductCreator.class);
    }
}
