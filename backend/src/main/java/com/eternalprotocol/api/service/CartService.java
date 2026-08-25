package com.eternalprotocol.api.service;

import com.eternalprotocol.api.dto.CartItemDto;
import com.eternalprotocol.api.dto.CartItemRequestDto;
import com.eternalprotocol.api.entity.CartItem;
import com.eternalprotocol.api.entity.Customer;
import com.eternalprotocol.api.entity.Product;
import com.eternalprotocol.api.exception.ResourceNotFoundException;
import com.eternalprotocol.api.repository.CartItemRepository;
import com.eternalprotocol.api.repository.CustomerRepository;
import com.eternalprotocol.api.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Business logic for a logged-in customer's saved cart.
 * <p>
 * Only applies to logged-in customers — guests keep an entirely
 * browser-side cart on the frontend, and nothing here is touched until
 * someone with an account is authenticated.
 * <p>
 * Every method is {@code @Transactional}. {@code CartItem.product} is a
 * "lazy" association, meaning it's only fetched from the database when
 * actually read — and {@link #toDto} reads it while building the
 * response. With {@code open-in-view: false} in this app's configuration,
 * that lazy read is only safe while the database session from the
 * repository call is still open, so the whole method — including the DTO
 * conversion — must run inside one transaction.
 */
@Service
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;

    /**
     * @param cartItemRepository access to cart line data
     * @param customerRepository access to customer data
     * @param productRepository  access to product data
     */
    public CartService(CartItemRepository cartItemRepository,
                        CustomerRepository customerRepository,
                        ProductRepository productRepository) {
        this.cartItemRepository = cartItemRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
    }

    /**
     * Gets a customer's full cart.
     *
     * @param customerId customer's database ID
     * @return the customer's cart lines
     */
    @Transactional(readOnly = true)
    public List<CartItemDto> getCart(Long customerId) {
        return cartItemRepository.findByCustomerId(customerId).stream().map(this::toDto).toList();
    }

    /**
     * Adds an item to the cart. If the same product/size/colour is
     * already present, its quantity is increased instead of creating a
     * duplicate line.
     *
     * @param customerId customer's database ID
     * @param request    item to add
     * @return the added or updated cart line
     * @throws ResourceNotFoundException if the customer or product doesn't exist
     */
    @Transactional
    public CartItemDto addOrUpdateItem(Long customerId, CartItemRequestDto request) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + request.productId()));

        CartItem item = cartItemRepository
                .findByCustomerIdAndProductIdAndSizeAndColour(customerId, request.productId(), request.size(), request.colour())
                .orElseGet(() -> new CartItem(customer, product, request.size(), request.colour(), 0));

        item.setQuantity(item.getQuantity() + request.quantity());
        return toDto(cartItemRepository.save(item));
    }

    /**
     * Removes one line from a customer's cart.
     *
     * @param customerId customer's database ID
     * @param itemId     cart line's ID
     * @throws ResourceNotFoundException if the cart line doesn't exist or doesn't belong to this customer
     */
    @Transactional
    public void removeItem(Long customerId, Long itemId) {
        CartItem item = cartItemRepository.findByIdAndCustomerId(itemId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));
        cartItemRepository.delete(item);
    }

    /**
     * Sets a cart line to an exact quantity. Different from
     * {@link #addOrUpdateItem}, which increments — this is what a "+"/"-"
     * stepper or a typed quantity field on the cart page should call.
     *
     * @param customerId customer's database ID
     * @param itemId     cart line's ID
     * @param quantity   the new exact quantity
     * @return the updated cart line
     * @throws ResourceNotFoundException if the cart line doesn't exist or doesn't belong to this customer
     */
    @Transactional
    public CartItemDto updateQuantity(Long customerId, Long itemId, int quantity) {
        CartItem item = cartItemRepository.findByIdAndCustomerId(itemId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));
        item.setQuantity(quantity);
        return toDto(cartItemRepository.save(item));
    }

    /**
     * Deletes every line in a customer's cart, e.g. right after a
     * successful checkout.
     *
     * @param customerId customer's database ID
     */
    @Transactional
    public void clearCart(Long customerId) {
        cartItemRepository.deleteByCustomerId(customerId);
    }

    /**
     * Gets a single cart line, scoped to its owning customer. Used by the
     * cart {@link com.eternalprotocol.api.command.CartCommand} classes to
     * snapshot a line's state before mutating it, so an undo can restore
     * exactly what was there before.
     *
     * @param customerId customer's database ID
     * @param itemId     cart line's ID
     * @return the matching cart line
     * @throws ResourceNotFoundException if the cart line doesn't exist or doesn't belong to this customer
     */
    @Transactional(readOnly = true)
    public CartItemDto getCartItem(Long customerId, Long itemId) {
        CartItem item = cartItemRepository.findByIdAndCustomerId(itemId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));
        return toDto(item);
    }

    /**
     * Converts a database entity into its public-facing DTO shape.
     *
     * @param item the entity to convert
     * @return the equivalent DTO
     */
    private CartItemDto toDto(CartItem item) {
        return new CartItemDto(
                item.getId(), item.getProduct().getId(), item.getProduct().getName(),
                item.getProduct().getPrice(), item.getSize(), item.getColour(),
                item.getQuantity(), item.getProduct().getImageUrl());
    }
}
