package com.eternalprotocol.api.controller;

import com.eternalprotocol.api.dto.ImageUploadResponseDto;
import com.eternalprotocol.api.dto.ProductDto;
import com.eternalprotocol.api.dto.ProductRequestDto;
import com.eternalprotocol.api.service.ImageUploadService;
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

    public ProductController(ProductService productService, ImageUploadService imageUploadService) {
        this.productService = productService;
        this.imageUploadService = imageUploadService;
    }

    // Public — Module 1's storefront only ever needs these two GETs.
    @GetMapping
    public ResponseEntity<List<ProductDto>> getAllProducts(
            @RequestParam(required = false) String colour,
            @RequestParam(required = false) String size,
            @RequestParam(required = false) String styleCode) {
        return ResponseEntity.ok(productService.getAllProducts(colour, size, styleCode));
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
     * public URL — the frontend then includes that URL as `imageUrl` in a
     * create or update product request. Deliberately a separate step
     * rather than baked into create/update: it lets the admin see the
     * uploaded image before submitting the rest of the form, and keeps
     * the (potentially large, binary) upload request separate from the
     * (small, JSON) product-details request.
     */
    @PostMapping(value = "/upload-image", consumes = "multipart/form-data")
    public ResponseEntity<ImageUploadResponseDto> uploadImage(@RequestParam("file") MultipartFile file) {
        String imageUrl = imageUploadService.uploadProductImage(file);
        return ResponseEntity.ok(new ImageUploadResponseDto(imageUrl));
    }
}
