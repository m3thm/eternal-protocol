package com.eternalprotocol.api.service;

import com.eternalprotocol.api.dto.ProductDto;
import com.eternalprotocol.api.dto.ProductRequestDto;
import com.eternalprotocol.api.entity.Product;
import com.eternalprotocol.api.exception.ResourceNotFoundException;
import com.eternalprotocol.api.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private Product sampleProduct;

    @BeforeEach
    void setUp() {
        sampleProduct = new Product();
        sampleProduct.setId(1L);
        sampleProduct.setStyleCode("PROTOCOL-001");
        sampleProduct.setName("Protocol 001 Oversized Tee");
        sampleProduct.setPrice(new BigDecimal("4500.00"));
        sampleProduct.setColour("Black");
        sampleProduct.setSize("M");
        sampleProduct.setStockQty(50);
        sampleProduct.setImageUrl("https://example.com/tee.png");
        sampleProduct.setDescription("Rise Through Discipline.");
    }

    @Test
    void getAllProducts_noFilters_passesAllNullsToSearch() {
        when(productRepository.search(null, null, null)).thenReturn(List.of(sampleProduct));

        List<ProductDto> result = productService.getAllProducts(null, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Protocol 001 Oversized Tee");
        assertThat(result.get(0).styleCode()).isEqualTo("PROTOCOL-001");
        verify(productRepository).search(null, null, null);
    }

    @Test
    void getAllProducts_colourFilterOnly_passesColourThrough() {
        when(productRepository.search(null, "Black", null)).thenReturn(List.of(sampleProduct));

        List<ProductDto> result = productService.getAllProducts("Black", null, null);

        assertThat(result).hasSize(1);
        verify(productRepository).search(null, "Black", null);
    }

    @Test
    void getAllProducts_sizeFilterOnly_passesSizeThrough() {
        when(productRepository.search(null, null, "M")).thenReturn(List.of(sampleProduct));

        List<ProductDto> result = productService.getAllProducts(null, "M", null);

        assertThat(result).hasSize(1);
        verify(productRepository).search(null, null, "M");
    }

    @Test
    void getAllProducts_styleCodeFilter_returnsAllVariantsOfThatStyle() {
        Product whiteVariant = new Product();
        whiteVariant.setId(2L);
        whiteVariant.setStyleCode("PROTOCOL-001");
        whiteVariant.setName("Protocol 001 Oversized Tee");
        whiteVariant.setPrice(new BigDecimal("4500.00"));
        whiteVariant.setColour("White");
        whiteVariant.setSize("M");
        whiteVariant.setStockQty(30);

        when(productRepository.search("PROTOCOL-001", null, null))
                .thenReturn(List.of(sampleProduct, whiteVariant));

        List<ProductDto> result = productService.getAllProducts(null, null, "PROTOCOL-001");

        assertThat(result).hasSize(2);
        assertThat(result).extracting(ProductDto::colour).containsExactlyInAnyOrder("Black", "White");
    }

    @Test
    void getAllProducts_allThreeFiltersCombined_passesAllThrough() {
        when(productRepository.search("PROTOCOL-001", "Black", "M")).thenReturn(List.of(sampleProduct));

        List<ProductDto> result = productService.getAllProducts("Black", "M", "PROTOCOL-001");

        assertThat(result).hasSize(1);
        verify(productRepository).search("PROTOCOL-001", "Black", "M");
    }

    @Test
    void getProductById_found_returnsDtoWithStyleCode() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));

        ProductDto result = productService.getProductById(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.styleCode()).isEqualTo("PROTOCOL-001");
        assertThat(result.price()).isEqualByComparingTo("4500.00");
    }

    @Test
    void getProductById_notFound_throwsResourceNotFoundException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void createProduct_savesAndReturnsNewProductWithStyleCode() {
        ProductRequestDto request = new ProductRequestDto(
                "PROTOCOL-002", "New Tee", new BigDecimal("3000.00"), "White", "L", 20, "img.png", "desc");
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(2L);
            return p;
        });

        ProductDto result = productService.createProduct(request);

        assertThat(result.id()).isEqualTo(2L);
        assertThat(result.styleCode()).isEqualTo("PROTOCOL-002");
        assertThat(result.name()).isEqualTo("New Tee");
        assertThat(result.colour()).isEqualTo("White");
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void updateProduct_existingProduct_updatesFieldsIncludingStyleCode() {
        ProductRequestDto request = new ProductRequestDto(
                "PROTOCOL-001-V2", "Updated Tee", new BigDecimal("5000.00"), "Grey", "XL", 10, "img2.png", "desc2");
        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductDto result = productService.updateProduct(1L, request);

        assertThat(result.styleCode()).isEqualTo("PROTOCOL-001-V2");
        assertThat(result.name()).isEqualTo("Updated Tee");
        assertThat(result.price()).isEqualByComparingTo("5000.00");
        assertThat(result.colour()).isEqualTo("Grey");
        assertThat(result.stockQty()).isEqualTo(10);
    }

    @Test
    void updateProduct_notFound_throwsResourceNotFoundException() {
        ProductRequestDto request = new ProductRequestDto(
                "STYLE-X", "X", BigDecimal.TEN, "Red", "S", 5, null, null);
        when(productRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.updateProduct(404L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteProduct_existingProduct_deletesIt() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));

        productService.deleteProduct(1L);

        verify(productRepository).delete(sampleProduct);
    }

    @Test
    void deleteProduct_notFound_throwsAndNeverCallsDelete() {
        when(productRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.deleteProduct(404L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(productRepository, never()).delete(any());
    }
}
