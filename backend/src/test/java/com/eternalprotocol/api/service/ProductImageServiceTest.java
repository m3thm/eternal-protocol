package com.eternalprotocol.api.service;

import com.eternalprotocol.api.dto.ProductImageDto;
import com.eternalprotocol.api.dto.ProductImageRequestDto;
import com.eternalprotocol.api.entity.Product;
import com.eternalprotocol.api.entity.ProductImage;
import com.eternalprotocol.api.exception.ResourceNotFoundException;
import com.eternalprotocol.api.repository.ProductImageRepository;
import com.eternalprotocol.api.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductImageServiceTest {

    @Mock
    private ProductImageRepository productImageRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private ImageUploadService imageUploadService;

    @InjectMocks
    private ProductImageService productImageService;

    private ProductImage newImage(Long id, String styleCode, String colour, String url, String publicId, int sortOrder, boolean cover) {
        ProductImage image = new ProductImage();
        image.setId(id);
        image.setStyleCode(styleCode);
        image.setColour(colour);
        image.setImageUrl(url);
        image.setCloudinaryPublicId(publicId);
        image.setSortOrder(sortOrder);
        image.setCover(cover);
        return image;
    }

    // ---- addImage ----

    @Test
    void addImage_firstPhotoForColour_becomesCoverAutomatically() {
        ProductImageRequestDto request = new ProductImageRequestDto("Black", "https://cdn/1.png", "pub_1", null);
        when(productImageRepository.countByStyleCodeAndColour("STYLE-1", "Black")).thenReturn(0L);
        when(productImageRepository.save(any(ProductImage.class))).thenAnswer(inv -> {
            ProductImage img = inv.getArgument(0);
            img.setId(10L);
            return img;
        });
        when(productImageRepository.findByStyleCodeAndColourAndCoverTrue("STYLE-1", "Black"))
                .thenReturn(Optional.of(newImage(10L, "STYLE-1", "Black", "https://cdn/1.png", "pub_1", 0, true)));
        when(productRepository.search("STYLE-1", "Black", null)).thenReturn(List.of());

        ProductImageDto result = productImageService.addImage("STYLE-1", request);

        assertThat(result.isCover()).isTrue();
        assertThat(result.sortOrder()).isEqualTo(0);

        ArgumentCaptor<ProductImage> captor = ArgumentCaptor.forClass(ProductImage.class);
        verify(productImageRepository).save(captor.capture());
        assertThat(captor.getValue().isCover()).isTrue();
        assertThat(captor.getValue().getCloudinaryPublicId()).isEqualTo("pub_1");
    }

    @Test
    void addImage_secondPhotoForColour_isNotAutomaticallyCover() {
        ProductImageRequestDto request = new ProductImageRequestDto("Black", "https://cdn/2.png", "pub_2", null);
        when(productImageRepository.countByStyleCodeAndColour("STYLE-1", "Black")).thenReturn(1L);
        when(productImageRepository.save(any(ProductImage.class))).thenAnswer(inv -> inv.getArgument(0));
        when(productImageRepository.findByStyleCodeAndColourAndCoverTrue("STYLE-1", "Black"))
                .thenReturn(Optional.of(newImage(9L, "STYLE-1", "Black", "https://cdn/1.png", "pub_1", 0, true)));
        when(productRepository.search("STYLE-1", "Black", null)).thenReturn(List.of());

        ProductImageDto result = productImageService.addImage("STYLE-1", request);

        assertThat(result.isCover()).isFalse();
        assertThat(result.sortOrder()).isEqualTo(1);
    }

    @Test
    void addImage_syncsCoverUrlOntoEverySizeRowOfThatColour() {
        ProductImageRequestDto request = new ProductImageRequestDto("Black", "https://cdn/1.png", "pub_1", null);
        when(productImageRepository.countByStyleCodeAndColour("STYLE-1", "Black")).thenReturn(0L);
        when(productImageRepository.save(any(ProductImage.class))).thenAnswer(inv -> inv.getArgument(0));
        when(productImageRepository.findByStyleCodeAndColourAndCoverTrue("STYLE-1", "Black"))
                .thenReturn(Optional.of(newImage(10L, "STYLE-1", "Black", "https://cdn/1.png", "pub_1", 0, true)));

        Product sizeM = new Product();
        sizeM.setId(1L);
        Product sizeL = new Product();
        sizeL.setId(2L);
        when(productRepository.search("STYLE-1", "Black", null)).thenReturn(List.of(sizeM, sizeL));

        productImageService.addImage("STYLE-1", request);

        assertThat(sizeM.getImageUrl()).isEqualTo("https://cdn/1.png");
        assertThat(sizeL.getImageUrl()).isEqualTo("https://cdn/1.png");
        verify(productRepository).saveAll(List.of(sizeM, sizeL));
    }

    // ---- setCoverImage ----

    @Test
    void setCoverImage_marksOnlyThatImageAsCoverAmongSiblings() {
        ProductImage target = newImage(2L, "STYLE-1", "Black", "https://cdn/2.png", "pub_2", 1, false);
        ProductImage current = newImage(1L, "STYLE-1", "Black", "https://cdn/1.png", "pub_1", 0, true);
        when(productImageRepository.findById(2L)).thenReturn(Optional.of(target));
        when(productImageRepository.findByStyleCodeAndColourOrderBySortOrderAsc("STYLE-1", "Black"))
                .thenReturn(List.of(current, target));
        when(productImageRepository.findByStyleCodeAndColourAndCoverTrue("STYLE-1", "Black"))
                .thenReturn(Optional.of(target));
        when(productRepository.search("STYLE-1", "Black", null)).thenReturn(List.of());

        ProductImageDto result = productImageService.setCoverImage(2L);

        assertThat(result.id()).isEqualTo(2L);
        assertThat(target.isCover()).isTrue();
        assertThat(current.isCover()).isFalse();
        verify(productImageRepository).saveAll(List.of(current, target));
    }

    @Test
    void setCoverImage_notFound_throwsResourceNotFoundException() {
        when(productImageRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productImageService.setCoverImage(404L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- deleteImage ----

    @Test
    void deleteImage_nonCoverPhoto_deletesFromCloudinaryAndDoesNotReassignCover() {
        ProductImage image = newImage(2L, "STYLE-1", "Black", "https://cdn/2.png", "pub_2", 1, false);
        when(productImageRepository.findById(2L)).thenReturn(Optional.of(image));
        when(productImageRepository.findByStyleCodeAndColourAndCoverTrue("STYLE-1", "Black"))
                .thenReturn(Optional.of(newImage(1L, "STYLE-1", "Black", "https://cdn/1.png", "pub_1", 0, true)));
        when(productRepository.search("STYLE-1", "Black", null)).thenReturn(List.of());

        productImageService.deleteImage(2L);

        verify(productImageRepository).delete(image);
        verify(imageUploadService).deleteProductImage("pub_2");
        // Wasn't the cover, so no need to look for a replacement.
        verify(productImageRepository, never()).findByStyleCodeAndColourOrderBySortOrderAsc(any(), any());
    }

    @Test
    void deleteImage_coverPhotoWithOthersRemaining_promotesNextPhotoToCover() {
        ProductImage cover = newImage(1L, "STYLE-1", "Black", "https://cdn/1.png", "pub_1", 0, true);
        ProductImage next = newImage(2L, "STYLE-1", "Black", "https://cdn/2.png", "pub_2", 1, false);
        when(productImageRepository.findById(1L)).thenReturn(Optional.of(cover));
        when(productImageRepository.findByStyleCodeAndColourOrderBySortOrderAsc("STYLE-1", "Black"))
                .thenReturn(List.of(next));
        when(productImageRepository.findByStyleCodeAndColourAndCoverTrue("STYLE-1", "Black"))
                .thenReturn(Optional.of(next));
        when(productRepository.search("STYLE-1", "Black", null)).thenReturn(List.of());

        productImageService.deleteImage(1L);

        verify(imageUploadService).deleteProductImage("pub_1");
        assertThat(next.isCover()).isTrue();
        verify(productImageRepository).save(next);
    }

    @Test
    void deleteImage_notFound_throwsResourceNotFoundException() {
        when(productImageRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productImageService.deleteImage(404L))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(imageUploadService);
    }

    // ---- deleteAllImagesForStyleAndColour ----

    @Test
    void deleteAllImagesForStyleAndColour_deletesEveryCloudinaryAssetAndDbRow() {
        ProductImage img1 = newImage(1L, "STYLE-1", "Black", "https://cdn/1.png", "pub_1", 0, true);
        ProductImage img2 = newImage(2L, "STYLE-1", "Black", "https://cdn/2.png", "pub_2", 1, false);
        when(productImageRepository.findByStyleCodeAndColourOrderBySortOrderAsc("STYLE-1", "Black"))
                .thenReturn(List.of(img1, img2));

        productImageService.deleteAllImagesForStyleAndColour("STYLE-1", "Black");

        verify(productImageRepository).deleteAll(List.of(img1, img2));
        verify(imageUploadService).deleteProductImage("pub_1");
        verify(imageUploadService).deleteProductImage("pub_2");
    }

    @Test
    void deleteAllImagesForStyleAndColour_noImages_doesNothing() {
        when(productImageRepository.findByStyleCodeAndColourOrderBySortOrderAsc("STYLE-1", "Black"))
                .thenReturn(List.of());

        productImageService.deleteAllImagesForStyleAndColour("STYLE-1", "Black");

        verify(productImageRepository, never()).deleteAll(any());
        verifyNoInteractions(imageUploadService);
    }

    // ---- reorderImages ----

    @Test
    void reorderImages_appliesNewSortOrderAndReturnsSortedList() {
        ProductImage img1 = newImage(1L, "STYLE-1", "Black", "https://cdn/1.png", "pub_1", 0, true);
        ProductImage img2 = newImage(2L, "STYLE-1", "Black", "https://cdn/2.png", "pub_2", 1, false);
        when(productImageRepository.findByStyleCodeAndColourOrderBySortOrderAsc("STYLE-1", "Black"))
                .thenReturn(List.of(img1, img2));
        when(productImageRepository.findByStyleCodeAndColourAndCoverTrue("STYLE-1", "Black"))
                .thenReturn(Optional.of(img1));
        when(productRepository.search("STYLE-1", "Black", null)).thenReturn(List.of());

        List<ProductImageDto> result = productImageService.reorderImages("STYLE-1", "Black", List.of(2L, 1L));

        assertThat(img2.getSortOrder()).isEqualTo(0);
        assertThat(img1.getSortOrder()).isEqualTo(1);
        assertThat(result).extracting(ProductImageDto::id).containsExactly(2L, 1L);
    }

    @Test
    void reorderImages_unknownImageId_throwsResourceNotFoundException() {
        when(productImageRepository.findByStyleCodeAndColourOrderBySortOrderAsc("STYLE-1", "Black"))
                .thenReturn(List.of());

        assertThatThrownBy(() -> productImageService.reorderImages("STYLE-1", "Black", List.of(999L)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- getCoverImageUrl ----

    @Test
    void getCoverImageUrl_present_returnsUrl() {
        when(productImageRepository.findByStyleCodeAndColourAndCoverTrue("STYLE-1", "Black"))
                .thenReturn(Optional.of(newImage(1L, "STYLE-1", "Black", "https://cdn/1.png", "pub_1", 0, true)));

        assertThat(productImageService.getCoverImageUrl("STYLE-1", "Black")).contains("https://cdn/1.png");
    }

    @Test
    void getCoverImageUrl_absent_returnsEmpty() {
        when(productImageRepository.findByStyleCodeAndColourAndCoverTrue("STYLE-1", "Black"))
                .thenReturn(Optional.empty());

        assertThat(productImageService.getCoverImageUrl("STYLE-1", "Black")).isEmpty();
    }
}
