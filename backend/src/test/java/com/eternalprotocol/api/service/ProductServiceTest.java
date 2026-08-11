package com.eternalprotocol.api.service;

import com.eternalprotocol.api.dto.ProductDto;
import com.eternalprotocol.api.dto.ProductRequestDto;
import com.eternalprotocol.api.entity.Product;
import com.eternalprotocol.api.exception.ProductInUseException;
import com.eternalprotocol.api.exception.ResourceNotFoundException;
import com.eternalprotocol.api.repository.CartItemRepository;
import com.eternalprotocol.api.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

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
    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private ProductImageService productImageService;

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
    void createProduct_noGalleryCoverYet_fallsBackToRequestImageUrl() {
        // No gallery photos uploaded for this style+colour yet (the normal
        // case for a brand-new style code) — productImageService returns
        // empty, so whatever the request explicitly provided is used as-is.
        ProductRequestDto request = new ProductRequestDto(
                "PROTOCOL-002", "New Tee", new BigDecimal("3000.00"), "White", "L", 20, "img.png", "desc");
        when(productImageService.getCoverImageUrl("PROTOCOL-002", "White")).thenReturn(Optional.empty());
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
        assertThat(result.imageUrl()).isEqualTo("img.png");
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void createProduct_galleryAlreadyHasCoverForThisStyleAndColour_usesGalleryCoverInstead() {
        // Admin uploaded gallery photos and picked a cover for this
        // style+colour before saving the product row itself — the gallery
        // cover wins over whatever (if anything) the request passed.
        ProductRequestDto request = new ProductRequestDto(
                "PROTOCOL-003", "Cover Test Tee", new BigDecimal("3000.00"), "Black", "M", 10, null, null);
        when(productImageService.getCoverImageUrl("PROTOCOL-003", "Black"))
                .thenReturn(Optional.of("https://cloudinary.example.com/gallery-cover.png"));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductDto result = productService.createProduct(request);

        assertThat(result.imageUrl()).isEqualTo("https://cloudinary.example.com/gallery-cover.png");
    }

    @Test
    void updateProduct_existingProduct_updatesFieldsIncludingStyleCode() {
        ProductRequestDto request = new ProductRequestDto(
                "PROTOCOL-001-V2", "Updated Tee", new BigDecimal("5000.00"), "Grey", "XL", 10, "img2.png", "desc2");
        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));
        when(productImageService.getCoverImageUrl("PROTOCOL-001-V2", "Grey")).thenReturn(Optional.empty());
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductDto result = productService.updateProduct(1L, request);

        assertThat(result.styleCode()).isEqualTo("PROTOCOL-001-V2");
        assertThat(result.name()).isEqualTo("Updated Tee");
        assertThat(result.price()).isEqualByComparingTo("5000.00");
        assertThat(result.colour()).isEqualTo("Grey");
        assertThat(result.stockQty()).isEqualTo(10);
        assertThat(result.imageUrl()).isEqualTo("img2.png");
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
    void deleteProduct_lastSizeForColour_clearsCartItemsAndPurgesGallery() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));
        // No other size rows left for PROTOCOL-001/Black after this delete.
        when(productRepository.search("PROTOCOL-001", "Black", null)).thenReturn(List.of());

        productService.deleteProduct(1L);

        // Cart items must be cleared before the delete, or the FK would block it.
        verify(cartItemRepository).deleteByProductId(1L);
        verify(productRepository).delete(sampleProduct);
        verify(productRepository).flush();
        // Last size for this colour is gone, so its photo gallery (and Cloudinary assets) get purged too.
        verify(productImageService).deleteAllImagesForStyleAndColour("PROTOCOL-001", "Black");
    }

    @Test
    void deleteProduct_otherSizesOfSameColourRemain_leavesGalleryAlone() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));
        // A Black/L row (say) still exists after deleting Black/M — the
        // colour's photos are still in use, so they must not be purged.
        Product remainingSize = new Product();
        remainingSize.setId(2L);
        remainingSize.setStyleCode("PROTOCOL-001");
        remainingSize.setColour("Black");
        remainingSize.setSize("L");
        when(productRepository.search("PROTOCOL-001", "Black", null)).thenReturn(List.of(remainingSize));

        productService.deleteProduct(1L);

        verify(productImageService, never()).deleteAllImagesForStyleAndColour(any(), any());
    }

    @Test
    void deleteProduct_notFound_throwsAndNeverCallsDelete() {
        when(productRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.deleteProduct(404L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(productRepository, never()).delete(any());
    }

    @Test
    void deleteProduct_referencedByPastOrder_throwsProductInUseExceptionWithClearMessage() {
        // Simulates the real-world case: an OrderItem foreign key blocks
        // the delete at flush time. This should surface as a clear,
        // actionable message instead of a raw DB error.
        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));
        doThrow(new DataIntegrityViolationException("FK violation")).when(productRepository).flush();

        assertThatThrownBy(() -> productService.deleteProduct(1L))
                .isInstanceOf(ProductInUseException.class)
                .hasMessageContaining("past orders");

        verify(cartItemRepository).deleteByProductId(1L);
        // The delete never actually succeeded, so the gallery must be untouched.
        verify(productImageService, never()).deleteAllImagesForStyleAndColour(any(), any());
    }
}
