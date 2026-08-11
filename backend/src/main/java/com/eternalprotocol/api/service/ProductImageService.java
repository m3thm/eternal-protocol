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
import java.util.Optional;

@Service
public class ProductImageService {

    private final ProductImageRepository productImageRepository;
    private final ProductRepository productRepository;
    private final ImageUploadService imageUploadService;

    public ProductImageService(ProductImageRepository productImageRepository, ProductRepository productRepository,
                                ImageUploadService imageUploadService) {
        this.productImageRepository = productImageRepository;
        this.productRepository = productRepository;
        this.imageUploadService = imageUploadService;
    }

    /** Full gallery for a style, every colour — frontend groups by colour. */
    public List<ProductImageDto> getImagesForStyle(String styleCode) {
        return productImageRepository.findByStyleCodeOrderByColourAscSortOrderAsc(styleCode)
                .stream().map(this::toDto).toList();
    }

    /** The current cover photo URL for a style+colour, if any — used by ProductService when saving a product row. */
    public Optional<String> getCoverImageUrl(String styleCode, String colour) {
        return productImageRepository.findByStyleCodeAndColourAndCoverTrue(styleCode, colour)
                .map(ProductImage::getImageUrl);
    }

    @Transactional
    public ProductImageDto addImage(String styleCode, ProductImageRequestDto request) {
        long existingCount = productImageRepository.countByStyleCodeAndColour(styleCode, request.colour());
        int sortOrder = request.sortOrder() != null ? request.sortOrder() : (int) existingCount;

        ProductImage image = new ProductImage();
        image.setStyleCode(styleCode);
        image.setColour(request.colour());
        image.setImageUrl(request.imageUrl());
        image.setCloudinaryPublicId(request.publicId());
        image.setSortOrder(sortOrder);
        // First photo uploaded for a new colour becomes its cover automatically —
        // the admin can change this afterward with setCoverImage.
        image.setCover(existingCount == 0);
        ProductImage saved = productImageRepository.save(image);

        syncCoverThumbnail(styleCode, request.colour());
        return toDto(saved);
    }

    /** Makes this image the cover for its style+colour, replacing whichever image held that role before. */
    @Transactional
    public ProductImageDto setCoverImage(Long imageId) {
        ProductImage image = productImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Product image not found: " + imageId));

        List<ProductImage> siblings = productImageRepository
                .findByStyleCodeAndColourOrderBySortOrderAsc(image.getStyleCode(), image.getColour());
        siblings.forEach(img -> img.setCover(img.getId().equals(imageId)));
        productImageRepository.saveAll(siblings);

        syncCoverThumbnail(image.getStyleCode(), image.getColour());
        return toDto(image);
    }

    @Transactional
    public void deleteImage(Long imageId) {
        ProductImage image = productImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Product image not found: " + imageId));
        String styleCode = image.getStyleCode();
        String colour = image.getColour();
        boolean wasCover = image.isCover();

        productImageRepository.delete(image);
        imageUploadService.deleteProductImage(image.getCloudinaryPublicId());

        if (wasCover) {
            // Promote whichever photo is now first in carousel order, so the
            // colour never ends up with photos but no cover.
            productImageRepository.findByStyleCodeAndColourOrderBySortOrderAsc(styleCode, colour)
                    .stream().findFirst()
                    .ifPresent(next -> {
                        next.setCover(true);
                        productImageRepository.save(next);
                    });
        }

        syncCoverThumbnail(styleCode, colour);
    }

    /**
     * Wipes every photo (DB row + Cloudinary asset) for a style+colour.
     * Called by ProductService.deleteProduct once the last size row for
     * that colour is gone — at that point nothing references these photos
     * any more, so leaving them in Cloudinary would just burn quota for no
     * reason.
     */
    @Transactional
    public void deleteAllImagesForStyleAndColour(String styleCode, String colour) {
        List<ProductImage> images = productImageRepository.findByStyleCodeAndColourOrderBySortOrderAsc(styleCode, colour);
        if (images.isEmpty()) {
            return;
        }
        productImageRepository.deleteAll(images);
        images.forEach(img -> imageUploadService.deleteProductImage(img.getCloudinaryPublicId()));
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
     * its imageUrl kept pointed at that colour's cover-flagged image (or
     * cleared if the gallery is now empty) whenever the gallery changes.
     */
    private void syncCoverThumbnail(String styleCode, String colour) {
        String coverUrl = getCoverImageUrl(styleCode, colour).orElse(null);

        List<Product> sizeRows = productRepository.search(styleCode, colour, null);
        sizeRows.forEach(p -> p.setImageUrl(coverUrl));
        productRepository.saveAll(sizeRows);
    }

    private ProductImageDto toDto(ProductImage image) {
        return new ProductImageDto(image.getId(), image.getColour(), image.getImageUrl(), image.getSortOrder(), image.isCover());
    }
}
