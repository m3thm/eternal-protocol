package com.eternalprotocol.api.controller;

import com.eternalprotocol.api.dto.ImageUploadResponseDto;
import com.eternalprotocol.api.dto.ProductDto;
import com.eternalprotocol.api.dto.ProductImageDto;
import com.eternalprotocol.api.dto.ProductImageRequestDto;
import com.eternalprotocol.api.dto.ProductRequestDto;
import com.eternalprotocol.api.dto.ReorderImagesRequestDto;
import com.eternalprotocol.api.entity.ProductCategory;
import com.eternalprotocol.api.service.ImageUploadService;
import com.eternalprotocol.api.service.ProductImageService;
import com.eternalprotocol.api.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final ImageUploadService imageUploadService;
    private final ProductImageService productImageService;

    public ProductController(ProductService productService, ImageUploadService imageUploadService,
                              ProductImageService productImageService) {
        this.productService = productService;
        this.imageUploadService = imageUploadService;
        this.productImageService = productImageService;
    }

    // Public — the storefront's category pages (MEN'S/WOMEN'S/ACCESSORIES,
    // each with colour/size filters) and the PDP's variant lookup both use this.
    @GetMapping
    public ResponseEntity<List<ProductDto>> getAllProducts(
            @RequestParam(required = false) String colour,
            @RequestParam(required = false) String size,
            @RequestParam(required = false) String styleCode,
            @RequestParam(required = false) ProductCategory category,
            @RequestParam(required = false) String subCategory) {
        return ResponseEntity.ok(productService.getAllProducts(colour, size, styleCode, category, subCategory));
    }

    /**
     * Public. Distinct subcategories that currently have products within
     * one category — what the navbar's MEN'S/WOMEN'S/ACCESSORIES dropdowns
     * show. Only ever lists subcategories that actually exist right now,
     * same principle as the colour/size filter options on the shop page.
     */
    @GetMapping("/subcategories")
    public ResponseEntity<List<String>> getSubCategories(@RequestParam ProductCategory category) {
        return ResponseEntity.ok(productService.getSubCategories(category));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDto> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    // Admin only — locked down in SecurityConfig, consumed by the admin dashboard UI.
    @PostMapping
    public ResponseEntity<ProductDto> createProduct(@Valid @RequestBody ProductRequestDto request) {
        return ResponseEntity.ok(productService.createProduct(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductDto> updateProduct(@PathVariable Long id, @Valid @RequestBody ProductRequestDto request) {
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Admin only. Uploads a product image to Cloudinary and returns its
     * public URL plus its Cloudinary public_id — the frontend then attaches
     * both to a style+colour's gallery via POST .../images. Deliberately a
     * separate step rather than baked into create/update: it lets the admin
     * see the uploaded image before submitting the rest of the form, and
     * keeps the (potentially large, binary) upload request separate from
     * the (small, JSON) product-details request.
     */
    @PostMapping(value = "/upload-image", consumes = "multipart/form-data")
    public ResponseEntity<ImageUploadResponseDto> uploadImage(@RequestParam("file") MultipartFile file) {
        ImageUploadService.UploadResult result = imageUploadService.uploadProductImage(file);
        return ResponseEntity.ok(new ImageUploadResponseDto(result.url(), result.publicId()));
    }

    // ---- Product image gallery (multiple images per style+colour, for the PDP carousel) ----

    /**
     * Public. Every image for every colour of this style, in carousel order
     * within each colour. Frontend groups the flat list by `colour`.
     */
    @GetMapping("/style/{styleCode}/images")
    public ResponseEntity<List<ProductImageDto>> getImagesForStyle(@PathVariable String styleCode) {
        return ResponseEntity.ok(productImageService.getImagesForStyle(styleCode));
    }

    /**
     * Admin only. Call /api/products/upload-image first to get the imageUrl
     * (and publicId), then attach it to this style+colour's gallery with
     * this endpoint. The cover photo for a product now comes from here —
     * see setCoverImage — rather than a separate manual upload.
     */
    @PostMapping("/style/{styleCode}/images")
    public ResponseEntity<ProductImageDto> addImage(@PathVariable String styleCode,
                                                      @Valid @RequestBody ProductImageRequestDto request) {
        return ResponseEntity.ok(productImageService.addImage(styleCode, request));
    }

    // Admin only — locked down in SecurityConfig alongside the other DELETE /api/products/** rules.
    // Also deletes the underlying Cloudinary asset (see ProductImageService.deleteImage).
    @DeleteMapping("/images/{imageId}")
    public ResponseEntity<Void> deleteImage(@PathVariable Long imageId) {
        productImageService.deleteImage(imageId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Admin only. Marks this gallery image as the cover for its style+colour
     * — the photo shown on the shop grid and as the PDP carousel's first
     * frame. Replaces the old separate "upload a cover photo" flow: the
     * cover is just one of the gallery photos, chosen here.
     */
    @PutMapping("/images/{imageId}/cover")
    public ResponseEntity<ProductImageDto> setCoverImage(@PathVariable Long imageId) {
        return ResponseEntity.ok(productImageService.setCoverImage(imageId));
    }

    /** Admin only. Body is the full list of image IDs for this colour, in the new order. */
    @PutMapping("/style/{styleCode}/images/reorder")
    public ResponseEntity<List<ProductImageDto>> reorderImages(@PathVariable String styleCode,
                                                                 @RequestParam String colour,
                                                                 @Valid @RequestBody ReorderImagesRequestDto request) {
        return ResponseEntity.ok(productImageService.reorderImages(styleCode, colour, request.orderedImageIds()));
    }
}
