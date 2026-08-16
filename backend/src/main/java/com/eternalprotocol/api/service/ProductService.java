package com.eternalprotocol.api.service;

import com.eternalprotocol.api.dto.ProductDto;
import com.eternalprotocol.api.dto.ProductRequestDto;
import com.eternalprotocol.api.entity.Product;
import com.eternalprotocol.api.entity.ProductCategory;
import com.eternalprotocol.api.exception.ProductInUseException;
import com.eternalprotocol.api.exception.ResourceNotFoundException;
import com.eternalprotocol.api.repository.CartItemRepository;
import com.eternalprotocol.api.repository.ProductRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Business logic for creating, searching, updating, and deleting products.
 */
@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductImageService productImageService;

    /**
     * @param productRepository   access to product data
     * @param cartItemRepository  used to clear cart lines when a product is deleted
     * @param productImageService keeps a product's cover photo in sync with its gallery
     */
    public ProductService(ProductRepository productRepository, CartItemRepository cartItemRepository,
                           ProductImageService productImageService) {
        this.productRepository = productRepository;
        this.cartItemRepository = cartItemRepository;
        this.productImageService = productImageService;
    }

    /**
     * Searches products by any combination of filters. Any filter left
     * null is simply not applied.
     *
     * @param colour      colour to filter by, or null
     * @param size        size to filter by, or null
     * @param styleCode   style code to filter by, or null
     * @param category    top-level category to filter by, or null
     * @param subCategory subcategory to filter by, or null
     * @return matching products
     */
    public List<ProductDto> getAllProducts(String colour, String size, String styleCode,
                                            ProductCategory category, String subCategory) {
        return productRepository.searchStorefront(styleCode, colour, size, category, subCategory)
                .stream().map(this::toDto).toList();
    }

    /**
     * Gets every distinct subcategory currently in use within one
     * category — powers the storefront navbar's per-category dropdown.
     *
     * @param category category to look up subcategories for
     * @return distinct subcategory names
     */
    public List<String> getSubCategories(ProductCategory category) {
        return productRepository.findDistinctSubCategoriesByCategory(category);
    }

    /**
     * Gets a single product variant by ID.
     *
     * @param id product variant's database ID
     * @return the matching product
     * @throws ResourceNotFoundException if no product exists with this ID
     */
    public ProductDto getProductById(Long id) {
        return toDto(findProductOrThrow(id));
    }

    /**
     * Creates a new product variant.
     *
     * @param request new product's details
     * @return the created product
     */
    public ProductDto createProduct(ProductRequestDto request) {
        Product product = new Product();
        applyRequest(product, request);
        return toDto(productRepository.save(product));
    }

    /**
     * Updates an existing product variant.
     *
     * @param id      product variant's database ID
     * @param request updated product details
     * @return the updated product
     * @throws ResourceNotFoundException if no product exists with this ID
     */
    public ProductDto updateProduct(Long id, ProductRequestDto request) {
        Product product = findProductOrThrow(id);
        applyRequest(product, request);
        return toDto(productRepository.save(product));
    }

    /**
     * Permanently deletes one product variant (one size+colour row).
     * <p>
     * Any live cart lines referencing it are cleared first — safe to do,
     * since a cart is just in-progress shopper state, not order history.
     * But if the product appears in any past order, the delete is refused
     * with a clear message instead of failing with a raw database error or
     * silently corrupting order history.
     * <p>
     * If this was the last remaining size for its style+colour, that
     * colour's whole photo gallery (and its Cloudinary files) is cleaned
     * up too, since nothing would reference those photos anymore. If other
     * sizes of the same colour still exist, the gallery is left alone
     * since they still display it.
     *
     * @param id product variant's database ID
     * @throws ResourceNotFoundException if no product exists with this ID
     * @throws ProductInUseException     if the product appears in past orders
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

    /**
     * Copies fields from a create/update request onto a product entity.
     * Also syncs {@code imageUrl} from the product's photo gallery if one
     * already exists, since the cover photo now comes from the gallery
     * (see {@code ProductGalleryEditor} on the frontend) rather than being
     * set manually. Falls back to whatever the request explicitly
     * provided if no gallery cover exists yet.
     *
     * @param product the entity to update
     * @param request the incoming request data
     */
    private void applyRequest(Product product, ProductRequestDto request) {
        product.setStyleCode(request.styleCode());
        product.setName(request.name());
        product.setCategory(request.category());
        product.setSubCategory(request.subCategory());
        product.setPrice(request.price());
        product.setColour(request.colour());
        product.setSize(request.size());
        product.setStockQty(request.stockQty());
        product.setDescription(request.description());

        product.setImageUrl(productImageService.getCoverImageUrl(request.styleCode(), request.colour())
                .orElse(request.imageUrl()));
    }

    /**
     * Looks up a product by ID or throws if it doesn't exist.
     *
     * @param id product variant's database ID
     * @return the matching product entity
     * @throws ResourceNotFoundException if no product exists with this ID
     */
    private Product findProductOrThrow(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
    }

    /**
     * Converts a database entity into its public-facing DTO shape.
     *
     * @param p the entity to convert
     * @return the equivalent DTO
     */
    private ProductDto toDto(Product p) {
        return new ProductDto(p.getId(), p.getStyleCode(), p.getName(), p.getCategory(), p.getSubCategory(),
                p.getPrice(), p.getColour(), p.getSize(), p.getStockQty(), p.getImageUrl(), p.getDescription());
    }
}
