package com.eternalprotocol.api.service;

import com.eternalprotocol.api.dto.ProductDto;
import com.eternalprotocol.api.dto.ProductRequestDto;
import com.eternalprotocol.api.entity.Product;
import com.eternalprotocol.api.entity.ProductCategory;
import com.eternalprotocol.api.entity.ProductStatus;
import com.eternalprotocol.api.exception.ProductInUseException;
import com.eternalprotocol.api.exception.ResourceNotFoundException;
import com.eternalprotocol.api.factory.product.ProductCreator;
import com.eternalprotocol.api.factory.product.ProductCreatorFactory;
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
    private final ProductCreatorFactory productCreatorFactory;

    /**
     * @param productRepository     access to product data
     * @param cartItemRepository    used to clear cart lines when a product is deleted
     * @param productImageService   keeps a product's cover photo in sync with its gallery
     * @param productCreatorFactory picks the right category-specific size rule (Factory Method — see that package)
     */
    public ProductService(ProductRepository productRepository, CartItemRepository cartItemRepository,
                           ProductImageService productImageService, ProductCreatorFactory productCreatorFactory) {
        this.productRepository = productRepository;
        this.cartItemRepository = cartItemRepository;
        this.productImageService = productImageService;
        this.productCreatorFactory = productCreatorFactory;
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
     * @param status      lifecycle status to filter by, or null. Storefront
     *                    pages (category pages, New Drops, Upcoming) always
     *                    pass this explicitly; leaving it null — as the
     *                    admin product table does — returns every product
     *                    regardless of status, newest first
     * @return matching products, newest first
     */
    public List<ProductDto> getAllProducts(String colour, String size, String styleCode,
                                            ProductCategory category, String subCategory, ProductStatus status) {
        return productRepository.searchStorefront(styleCode, colour, size, category, subCategory, status)
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
     * <p>
     * Built via {@link ProductCreatorFactory} (Factory Method — see that
     * package): which creator subclass runs depends on
     * {@code request.category()}, and decides how this variant's
     * {@code size} is resolved — e.g. accessories default to "One Size"
     * when none is given, apparel requires one explicitly.
     *
     * @param request new product's details
     * @return the created product
     * @throws IllegalArgumentException if this category requires a size and none was given
     */
    public ProductDto createProduct(ProductRequestDto request) {
        Product product = productCreatorFactory.creatorFor(request.category()).createProduct(request);
        syncCoverImage(product, request);
        return toDto(productRepository.save(product));
    }

    /**
     * Updates an existing product variant.
     *
     * @param id      product variant's database ID
     * @param request updated product details
     * @return the updated product
     * @throws ResourceNotFoundException if no product exists with this ID
     * @throws IllegalArgumentException  if this category requires a size and none was given
     */
    public ProductDto updateProduct(Long id, ProductRequestDto request) {
        Product product = findProductOrThrow(id);
        productCreatorFactory.creatorFor(request.category()).applyCommonFields(product, request);
        syncCoverImage(product, request);
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
     * Syncs {@code imageUrl} from the product's photo gallery if one
     * already exists, since the cover photo now comes from the gallery
     * (see {@code ProductGalleryEditor} on the frontend) rather than being
     * set manually. Falls back to whatever the request explicitly
     * provided if no gallery cover exists yet.
     * <p>
     * Every other field is already set by this point — by
     * {@link ProductCreator#createProduct} for a new product, or
     * {@link ProductCreator#applyCommonFields} for an update — so this
     * only ever touches {@code imageUrl}.
     *
     * @param product the entity to update
     * @param request the incoming request data
     */
    private void syncCoverImage(Product product, ProductRequestDto request) {
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
                p.getPrice(), p.getColour(), p.getSize(), p.getStockQty(), p.getImageUrl(), p.getDescription(),
                p.getStatus(), p.isShowPrice(), p.getCreatedAt());
    }
}
