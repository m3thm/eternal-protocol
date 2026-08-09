package com.eternalprotocol.api.service;

import com.eternalprotocol.api.dto.ProductImageDto;
import com.eternalprotocol.api.dto.ProductImageRequestDto;
import com.eternalprotocol.api.entity.Product;
import com.eternalprotocol.api.entity.ProductImage;
import com.eternalprotocol.api.exception.ResourceNotFoundException;
import com.eternalprotocol.api.repository.ProductImageRepository;
import com.eternalprotocol.api.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductImageService {

    private final ProductImageRepository productImageRepository;
    private final ProductRepository productRepository;

    public ProductImageService(ProductImageRepository productImageRepository, ProductRepository productRepository) {
        this.productImageRepository = productImageRepository;
        this.productRepository = productRepository;
    }

    /** Full gallery for a style, grouped in the response as a flat ordered list — frontend groups by colour. */
    public List<ProductImageDto> getImagesForStyle(String styleCode) {
        return productImageRepository.findByStyleCodeOrderByColourAscSortOrderAsc(styleCode)
                .stream().map(this::toDto).toList();
    }

    @Transactional
    public ProductImageDto addImage(String styleCode, ProductImageRequestDto request) {
        int sortOrder = request.sortOrder() != null
                ? request.sortOrder()
                : (int) productImageRepository.countByStyleCodeAndColour(styleCode, request.colour());

        ProductImage image = new ProductImage();
        image.setStyleCode(styleCode);
        image.setColour(request.colour());
        image.setImageUrl(request.imageUrl());
        image.setSortOrder(sortOrder);
        ProductImage saved = productImageRepository.save(image);

        syncCoverThumbnail(styleCode, request.colour());
        return toDto(saved);
    }

    @Transactional
    public void deleteImage(Long imageId) {
        ProductImage image = productImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Product image not found: " + imageId));
        String styleCode = image.getStyleCode();
        String colour = image.getColour();
        productImageRepository.delete(image);

        syncCoverThumbnail(styleCode, colour);
    }

    @Transactional
    public List<ProductImageDto> reorderImages(String styleCode, String colour, List<Long> orderedImageIds) {
        List<ProductImage> images = productImageRepository.findByStyleCodeAndColourOrderBySortOrderAsc(styleCode, colour);

        for (int position = 0; position < orderedImageIds.size(); position++) {
            Long id = orderedImageIds.get(position);
            ProductImage match = images.stream()
                    .filter(img -> img.getId().equals(id))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("Product image not found: " + id));
            match.setSortOrder(position);
        }
        productImageRepository.saveAll(images);

        syncCoverThumbnail(styleCode, colour);
        return images.stream()
                .sorted((a, b) -> a.getSortOrder().compareTo(b.getSortOrder()))
                .map(this::toDto).toList();
    }

    /**
     * The Shop grid (ProductCard) reads Product.imageUrl directly rather than
     * joining against the gallery table, so every size row of a colour needs
     * its imageUrl kept pointed at that colour's first gallery image (or
     * cleared if the gallery is now empty) whenever the gallery changes.
     */
    private void syncCoverThumbnail(String styleCode, String colour) {
        List<ProductImage> remaining = productImageRepository
                .findByStyleCodeAndColourOrderBySortOrderAsc(styleCode, colour);
        String coverUrl = remaining.isEmpty() ? null : remaining.get(0).getImageUrl();

        List<Product> sizeRows = productRepository.search(styleCode, colour, null);
        sizeRows.forEach(p -> p.setImageUrl(coverUrl));
        productRepository.saveAll(sizeRows);
    }

    private ProductImageDto toDto(ProductImage image) {
        return new ProductImageDto(image.getId(), image.getColour(), image.getImageUrl(), image.getSortOrder());
    }
}
