package com.eternalprotocol.api.service;

import com.eternalprotocol.api.dto.ProductDto;
import com.eternalprotocol.api.dto.ProductRequestDto;
import com.eternalprotocol.api.entity.Product;
import com.eternalprotocol.api.exception.ResourceNotFoundException;
import com.eternalprotocol.api.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<ProductDto> getAllProducts(String colour, String size, String styleCode) {
        return productRepository.search(styleCode, colour, size).stream().map(this::toDto).toList();
    }

    public ProductDto getProductById(Long id) {
        return toDto(findProductOrThrow(id));
    }

    public ProductDto createProduct(ProductRequestDto request) {
        Product product = new Product();
        applyRequest(product, request);
        return toDto(productRepository.save(product));
    }

    public ProductDto updateProduct(Long id, ProductRequestDto request) {
        Product product = findProductOrThrow(id);
        applyRequest(product, request);
        return toDto(productRepository.save(product));
    }

    public void deleteProduct(Long id) {
        Product product = findProductOrThrow(id);
        productRepository.delete(product);
    }

    private void applyRequest(Product product, ProductRequestDto request) {
        product.setStyleCode(request.styleCode());
        product.setName(request.name());
        product.setPrice(request.price());
        product.setColour(request.colour());
        product.setSize(request.size());
        product.setStockQty(request.stockQty());
        product.setImageUrl(request.imageUrl());
        product.setDescription(request.description());
    }

    private Product findProductOrThrow(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
    }

    private ProductDto toDto(Product p) {
        return new ProductDto(p.getId(), p.getStyleCode(), p.getName(), p.getPrice(), p.getColour(),
                p.getSize(), p.getStockQty(), p.getImageUrl(), p.getDescription());
    }
}
