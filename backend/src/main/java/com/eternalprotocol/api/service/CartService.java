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
 * Persisted cart, scoped to a logged-in customer only (guests keep an
 * entirely client-side cart — see frontend CartContext, nothing here is
 * touched until someone with an account is authenticated).
 *
 * Every method is @Transactional: CartItem.product is a LAZY association,
 * and toDto() reads it — with `open-in-view: false`, that's only safe
 * while the originating JPA session is still open, which requires the DTO
 * mapping to happen inside the same transactional method rather than after
 * the repository call returns.
 */
@Service
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;

    public CartService(CartItemRepository cartItemRepository,
                        CustomerRepository customerRepository,
                        ProductRepository productRepository) {
        this.cartItemRepository = cartItemRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<CartItemDto> getCart(Long customerId) {
        return cartItemRepository.findByCustomerId(customerId).stream().map(this::toDto).toList();
    }

    /** Adds a new line, or increments quantity if the same product/size/colour is already in the cart. */
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

    @Transactional
    public void removeItem(Long customerId, Long itemId) {
        CartItem item = cartItemRepository.findByIdAndCustomerId(itemId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));
        cartItemRepository.delete(item);
    }

    /** Sets the line to an exact quantity — distinct from addOrUpdateItem,
     *  which increments. This is what a "-"/"+" stepper or a typed quantity
     *  input on the cart page should call. */
    @Transactional
    public CartItemDto updateQuantity(Long customerId, Long itemId, int quantity) {
        CartItem item = cartItemRepository.findByIdAndCustomerId(itemId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));
        item.setQuantity(quantity);
        return toDto(cartItemRepository.save(item));
    }

    @Transactional
    public void clearCart(Long customerId) {
        cartItemRepository.deleteByCustomerId(customerId);
    }

    private CartItemDto toDto(CartItem item) {
        return new CartItemDto(
                item.getId(), item.getProduct().getId(), item.getProduct().getName(),
                item.getProduct().getPrice(), item.getSize(), item.getColour(),
                item.getQuantity(), item.getProduct().getImageUrl());
    }
}
