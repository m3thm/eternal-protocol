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

/**
 * Business logic for a product style's photo gallery: adding, deleting,
 * reordering images, and choosing a cover photo per colour.
 */
@Service
public class ProductImageService {

    private final ProductImageRepository productImageRepository;
    private final ProductRepository productRepository;
    private final ImageUploadService imageUploadService;

    /**
     * @param productImageRepository access to gallery image data
     * @param productRepository      access to product data, used to keep cover thumbnails in sync
     * @param imageUploadService     deletes the underlying Cloudinary asset when an image is removed
     */
    public ProductImageService(ProductImageRepository productImageRepository, ProductRepository productRepository,
                                ImageUploadService imageUploadService) {
        this.productImageRepository = productImageRepository;
        this.productRepository = productRepository;
        this.imageUploadService = imageUploadService;
    }

    /**
     * Gets the full photo gallery for a style, every colour included. The
     * frontend groups the flat list by colour to build each colour's
     * carousel.
     *
     * @param styleCode style to fetch images for
     * @return all images for that style, in display order within each colour
     */
    public List<ProductImageDto> getImagesForStyle(String styleCode) {
        return productImageRepository.findByStyleCodeOrderByColourAscSortOrderAsc(styleCode)
                .stream().map(this::toDto).toList();
    }

    /**
     * Gets the current cover photo URL for a style/colour, if one is set.
     * Used by {@code ProductService} when saving a product row, so
     * {@code Product.imageUrl} always matches the gallery's chosen cover.
     *
     * @param styleCode style to check
     * @param colour    colour to check
     * @return the cover image's URL, or empty if none is set yet
     */
    public Optional<String> getCoverImageUrl(String styleCode, String colour) {
        return productImageRepository.findByStyleCodeAndColourAndCoverTrue(styleCode, colour)
                .map(ProductImage::getImageUrl);
    }

    /**
     * Adds a new image to a style/colour's gallery. The very first image
     * uploaded for a new colour automatically becomes its cover photo; the
     * admin can change this later with {@link #setCoverImage}.
     *
     * @param styleCode style to add the image to
     * @param request   the already-uploaded image's URL/ID, target colour, and optional position
     * @return the newly added image
     */
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
        image.setCover(existingCount == 0);
        ProductImage saved = productImageRepository.save(image);

        syncCoverThumbnail(styleCode, request.colour());
        return toDto(saved);
    }

    /**
     * Makes one image the cover photo for its style/colour, replacing
     * whichever image previously held that role.
     *
     * @param imageId image's database ID
     * @return the now-cover image
     * @throws ResourceNotFoundException if no image exists with this ID
     */
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

    /**
     * Permanently deletes one gallery image, including its file on
     * Cloudinary. If the deleted image was the cover photo, whichever
     * image is now first in display order is automatically promoted to
     * cover, so a colour with photos never ends up without one.
     *
     * @param imageId image's database ID
     * @throws ResourceNotFoundException if no image exists with this ID
     */
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
     * Deletes every photo (database row and Cloudinary asset) for a
     * style/colour. Called by {@code ProductService.deleteProduct} once
     * the last size row for that colour is gone — at that point nothing
     * references these photos anymore, so leaving them on Cloudinary would
     * just burn storage quota for no reason.
     *
     * @param styleCode style to clear images for
     * @param colour    colour to clear images for
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

    /**
     * Changes the display order of one colour's photo gallery.
     *
     * @param styleCode       style being reordered
     * @param colour          colour being reordered
     * @param orderedImageIds image IDs listed in their new desired order
     * @return the gallery in its new order
     * @throws ResourceNotFoundException if one of the given image IDs doesn't exist in this gallery
     */
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
     * Keeps every size row's {@code Product.imageUrl} pointed at its
     * colour's current cover image (or clears it if the gallery is now
     * empty). This exists because the shop grid reads
     * {@code Product.imageUrl} directly rather than joining against the
     * gallery table, so it must be kept up to date whenever the gallery
     * changes.
     *
     * @param styleCode style whose thumbnail needs syncing
     * @param colour    colour whose thumbnail needs syncing
     */
    private void syncCoverThumbnail(String styleCode, String colour) {
        String coverUrl = getCoverImageUrl(styleCode, colour).orElse(null);

        List<Product> sizeRows = productRepository.search(styleCode, colour, null);
        sizeRows.forEach(p -> p.setImageUrl(coverUrl));
        productRepository.saveAll(sizeRows);
    }

    /**
     * Converts a database entity into its public-facing DTO shape.
     *
     * @param image the entity to convert
     * @return the equivalent DTO
     */
    private ProductImageDto toDto(ProductImage image) {
        return new ProductImageDto(image.getId(), image.getColour(), image.getImageUrl(), image.getSortOrder(), image.isCover());
    }
}
