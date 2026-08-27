package com.eternalprotocol.api.controller;

import com.eternalprotocol.api.dto.ImageUploadResponseDto;
import com.eternalprotocol.api.dto.ProductDto;
import com.eternalprotocol.api.dto.ProductImageDto;
import com.eternalprotocol.api.dto.ProductImageRequestDto;
import com.eternalprotocol.api.dto.ProductRequestDto;
import com.eternalprotocol.api.dto.ReorderImagesRequestDto;
import com.eternalprotocol.api.entity.ProductCategory;
import com.eternalprotocol.api.entity.ProductStatus;
import com.eternalprotocol.api.service.ImageUploadService;
import com.eternalprotocol.api.service.ProductImageService;
import com.eternalprotocol.api.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * REST endpoints for browsing the storefront and, for admins, managing
 * products and their photo galleries.
 * <p>
 * Read endpoints (GET) are public so anyone can browse the shop. Write
 * endpoints (POST/PUT/DELETE) are restricted to the ADMIN role — see the
 * rules in {@code SecurityConfig}.
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final ImageUploadService imageUploadService;
    private final ProductImageService productImageService;

    /**
     * @param productService      product CRUD and search logic
     * @param imageUploadService  handles raw file uploads to Cloudinary
     * @param productImageService manages a product's photo gallery
     */
    public ProductController(ProductService productService, ImageUploadService imageUploadService,
                              ProductImageService productImageService) {
        this.productService = productService;
        this.imageUploadService = imageUploadService;
        this.productImageService = productImageService;
    }

    /**
     * Public. Lists products, optionally filtered. Powers both the
     * category pages (MEN'S/WOMEN'S/ACCESSORIES with colour/size filters)
     * and the product detail page's variant lookup. Any filter left out is
     * simply not applied.
     *
     * @param colour      colour to filter by, optional
     * @param size        size to filter by, optional
     * @param styleCode   style code to filter by, optional
     * @param category    top-level category to filter by, optional
     * @param subCategory subcategory to filter by, optional
     * @param status      lifecycle status to filter by, optional. The
     *                    storefront (category pages, New Drops, Upcoming)
     *                    always passes this; the admin product table leaves
     *                    it out to see every product regardless of status
     * @return matching products, newest first
     */
    @GetMapping
    public ResponseEntity<List<ProductDto>> getAllProducts(
            @RequestParam(required = false) String colour,
            @RequestParam(required = false) String size,
            @RequestParam(required = false) String styleCode,
            @RequestParam(required = false) ProductCategory category,
            @RequestParam(required = false) String subCategory,
            @RequestParam(required = false) ProductStatus status) {
        return ResponseEntity.ok(productService.getAllProducts(colour, size, styleCode, category, subCategory, status));
    }

    /**
     * Public. Distinct subcategories that currently have products within
     * one category — powers the navbar's per-category dropdown. Only ever
     * lists subcategories that actually exist right now, the same way the
     * shop page's colour/size filter options work.
     *
     * @param category category to look up subcategories for
     * @return distinct subcategory names
     */
    @GetMapping("/subcategories")
    public ResponseEntity<List<String>> getSubCategories(@RequestParam ProductCategory category) {
        return ResponseEntity.ok(productService.getSubCategories(category));
    }

    /**
     * Public. Gets a single product variant by ID.
     *
     * @param id product variant's ID
     * @return the matching product
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductDto> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    /**
     * Admin only. Creates a new product variant.
     *
     * @param request new product's details
     * @return the created product
     */
    @PostMapping
    public ResponseEntity<ProductDto> createProduct(@Valid @RequestBody ProductRequestDto request) {
        return ResponseEntity.ok(productService.createProduct(request));
    }

    /**
     * Admin only. Updates an existing product variant.
     *
     * @param id      product variant's ID
     * @param request updated product details
     * @return the updated product
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProductDto> updateProduct(@PathVariable Long id, @Valid @RequestBody ProductRequestDto request) {
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    /**
     * Admin only. Permanently deletes a product variant. Fails if the
     * product is referenced by any past order — see
     * {@code ProductInUseException}.
     *
     * @param id product variant's ID
     * @return empty 204 response
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Admin only. Uploads a raw image file to Cloudinary and returns its
     * public URL and Cloudinary ID. This is a separate step from attaching
     * the image to a product ({@link #addImage}) — it lets the admin
     * preview the uploaded photo before submitting the rest of the form,
     * and keeps the large binary upload request separate from the small
     * JSON "attach this image" request.
     *
     * @param file the image file to upload
     * @return the uploaded image's URL and Cloudinary ID
     */
    @PostMapping(value = "/upload-image", consumes = "multipart/form-data")
    public ResponseEntity<ImageUploadResponseDto> uploadImage(@RequestParam("file") MultipartFile file) {
        ImageUploadService.UploadResult result = imageUploadService.uploadProductImage(file);
        return ResponseEntity.ok(new ImageUploadResponseDto(result.url(), result.publicId()));
    }

    /**
     * Public. Gets every gallery image for every colour of a style, in
     * display order within each colour. The frontend groups the flat list
     * by colour to build each colour's photo carousel.
     *
     * @param styleCode style to fetch images for
     * @return all images for that style
     */
    @GetMapping("/style/{styleCode}/images")
    public ResponseEntity<List<ProductImageDto>> getImagesForStyle(@PathVariable String styleCode) {
        return ResponseEntity.ok(productImageService.getImagesForStyle(styleCode));
    }

    /**
     * Admin only. Attaches an already-uploaded image (from
     * {@link #uploadImage}) to a style/colour's gallery.
     *
     * @param styleCode style to attach the image to
     * @param request   the uploaded image's URL/ID and target colour
     * @return the newly added gallery image
     */
    @PostMapping("/style/{styleCode}/images")
    public ResponseEntity<ProductImageDto> addImage(@PathVariable String styleCode,
                                                      @Valid @RequestBody ProductImageRequestDto request) {
        return ResponseEntity.ok(productImageService.addImage(styleCode, request));
    }

    /**
     * Admin only. Permanently deletes a gallery image, including its file
     * on Cloudinary.
     *
     * @param imageId image's database ID
     * @return empty 204 response
     */
    @DeleteMapping("/images/{imageId}")
    public ResponseEntity<Void> deleteImage(@PathVariable Long imageId) {
        productImageService.deleteImage(imageId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Admin only. Marks a gallery image as the cover photo for its
     * style/colour — the image shown on the shop grid and as the first
     * photo on the product detail page.
     *
     * @param imageId image's database ID
     * @return the updated image
     */
    @PutMapping("/images/{imageId}/cover")
    public ResponseEntity<ProductImageDto> setCoverImage(@PathVariable Long imageId) {
        return ResponseEntity.ok(productImageService.setCoverImage(imageId));
    }

    /**
     * Admin only. Changes the display order of one colour's photo gallery.
     *
     * @param styleCode style being reordered
     * @param colour    colour being reordered
     * @param request   image IDs in their new desired order
     * @return the gallery in its new order
     */
    @PutMapping("/style/{styleCode}/images/reorder")
    public ResponseEntity<List<ProductImageDto>> reorderImages(@PathVariable String styleCode,
                                                                 @RequestParam String colour,
                                                                 @Valid @RequestBody ReorderImagesRequestDto request) {
        return ResponseEntity.ok(productImageService.reorderImages(styleCode, colour, request.orderedImageIds()));
    }
}
