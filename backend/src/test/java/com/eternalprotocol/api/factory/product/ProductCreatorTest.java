package com.eternalprotocol.api.factory.product;

import com.eternalprotocol.api.dto.ProductRequestDto;
import com.eternalprotocol.api.entity.Product;
import com.eternalprotocol.api.entity.ProductCategory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductCreatorTest {

    private ProductRequestDto request(ProductCategory category, String size) {
        return new ProductRequestDto("PRT-001", "Protocol Tee", category, "T-Shirts",
                new BigDecimal("4500"), "Black", size, 10, null, "A tee.");
    }

    // --- ApparelProductCreator ---

    @Test
    void apparelProductCreator_explicitSize_isKept() {
        ProductCreator creator = new ApparelProductCreator();

        Product product = creator.createProduct(request(ProductCategory.MENS, "M"));

        assertThat(product.getSize()).isEqualTo("M");
    }

    @Test
    void apparelProductCreator_blankSize_throwsIllegalArgumentException() {
        ProductCreator creator = new ApparelProductCreator();

        assertThatThrownBy(() -> creator.createProduct(request(ProductCategory.WOMENS, " ")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void apparelProductCreator_nullSize_throwsIllegalArgumentException() {
        ProductCreator creator = new ApparelProductCreator();

        assertThatThrownBy(() -> creator.createProduct(request(ProductCategory.MENS, null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- AccessoryProductCreator ---

    @Test
    void accessoryProductCreator_blankSize_defaultsToOneSize() {
        ProductCreator creator = new AccessoryProductCreator();

        Product product = creator.createProduct(request(ProductCategory.ACCESSORIES, null));

        assertThat(product.getSize()).isEqualTo("One Size");
    }

    @Test
    void accessoryProductCreator_explicitSize_isRespected() {
        ProductCreator creator = new AccessoryProductCreator();

        Product product = creator.createProduct(request(ProductCategory.ACCESSORIES, "Adjustable"));

        assertThat(product.getSize()).isEqualTo("Adjustable");
    }

    // --- Shared template behaviour (createProduct) ---

    @Test
    void createProduct_copiesEveryCommonFieldOntoANewProduct() {
        ProductCreator creator = new ApparelProductCreator();

        Product product = creator.createProduct(request(ProductCategory.MENS, "L"));

        assertThat(product.getStyleCode()).isEqualTo("PRT-001");
        assertThat(product.getName()).isEqualTo("Protocol Tee");
        assertThat(product.getCategory()).isEqualTo(ProductCategory.MENS);
        assertThat(product.getSubCategory()).isEqualTo("T-Shirts");
        assertThat(product.getPrice()).isEqualByComparingTo("4500");
        assertThat(product.getColour()).isEqualTo("Black");
        assertThat(product.getStockQty()).isEqualTo(10);
        assertThat(product.getDescription()).isEqualTo("A tee.");
    }

    @Test
    void applyCommonFields_onAnExistingProduct_updatesItInPlace() {
        ProductCreator creator = new AccessoryProductCreator();
        Product existing = new Product();
        existing.setId(7L);

        creator.applyCommonFields(existing, request(ProductCategory.ACCESSORIES, null));

        assertThat(existing.getId()).isEqualTo(7L); // untouched
        assertThat(existing.getSize()).isEqualTo("One Size");
        assertThat(existing.getName()).isEqualTo("Protocol Tee");
    }
}
