package com.eternalprotocol.api.service;

import com.eternalprotocol.api.dto.ProductDto;
import com.eternalprotocol.api.dto.ProductRequestDto;
import com.eternalprotocol.api.entity.Product;
import com.eternalprotocol.api.exception.ProductInUseException;
import com.eternalprotocol.api.exception.ResourceNotFoundException;
import com.eternalprotocol.api.repository.CartItemRepository;
import com.eternalprotocol.api.repository.ProductRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductImageService productImageService;

    public ProductService(ProductRepository productRepository, CartItemRepository cartItemRepository,
                           ProductImageService productImageService) {
        this.productRepository = productRepository;
        this.cartItemRepository = cartItemRepository;
        this.productImageService = productImageService;
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

    /**
     * Hard-deletes a Product (one size+colour row). Live cart entries
     * referencing it are cleared first (safe — carts are just in-progress
     * shopper state), but if the product appears in any past order, the
     * delete is refused with a clear message rather than either failing
     * with a raw DB error or silently corrupting order history.
     * <p>
     * If this was the last remaining size for its style+colour, that
     * colour's whole photo gallery (and its Cloudinary assets) is cleaned
     * up too — nothing would reference those photos any more, so leaving
     * them in Cloudinary would just burn storage quota for no reason. If
     * other sizes of the same colour still exist, the gallery is left
     * alone since they still display it.
     */
    @Transactional
    public void deleteProduct(Long id) {
        Product product = findProductOrThrow(id);
        String styleCode = product.getStyleCode();
        String colour = product.getColour();

        cartItemRepository.deleteByProductId(id);
        try {
            productRepository.delete(product);
            productRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ProductInUseException(
                    "\"" + product.getName() + "\" (" + product.getColour() + "/" + product.getSize()
                            + ") can't be deleted because it appears in past orders. "
                            + "Set its stock to 0 instead to hide it from the shop.");
        }

        boolean colourStillInUse = !productRepository.search(styleCode, colour, null).isEmpty();
        if (!colourStillInUse) {
            productImageService.deleteAllImagesForStyleAndColour(styleCode, colour);
        }
    }

    private void applyRequest(Product product, ProductRequestDto request) {
        product.setStyleCode(request.styleCode());
        product.setName(request.name());
        product.setPrice(request.price());
        product.setColour(request.colour());
        product.setSize(request.size());
        product.setStockQty(request.stockQty());
        product.setDescription(request.description());

        // The cover photo comes from the image gallery (see
        // ProductGalleryEditor on the frontend), not a manual URL — if this
        // style+colour already has a gallery with a cover set, use it.
        // Otherwise fall back to whatever the request explicitly provided
        // (e.g. an older API caller, or a product created before any
        // gallery photos were uploaded for its colour).
        product.setImageUrl(productImageService.getCoverImageUrl(request.styleCode(), request.colour())
                .orElse(request.imageUrl()));
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
