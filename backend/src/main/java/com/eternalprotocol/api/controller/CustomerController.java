package com.eternalprotocol.api.controller;

import com.eternalprotocol.api.dto.CartItemDto;
import com.eternalprotocol.api.dto.CartItemRequestDto;
import com.eternalprotocol.api.dto.CustomerProfileDto;
import com.eternalprotocol.api.dto.OrderDetailDto;
import com.eternalprotocol.api.dto.UpdateCartItemQuantityRequestDto;
import com.eternalprotocol.api.dto.UpdateCustomerProfileRequestDto;
import com.eternalprotocol.api.security.CurrentUser;
import com.eternalprotocol.api.service.CartService;
import com.eternalprotocol.api.service.CustomerService;
import com.eternalprotocol.api.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Every endpoint here identifies "which customer" from the authenticated
 * JWT via CurrentUser — never from a path variable — so one customer can
 * never read another customer's profile, orders, or cart.
 */
@RestController
@RequestMapping("/api/customers/me")
public class CustomerController {

    private final CustomerService customerService;
    private final OrderService orderService;
    private final CartService cartService;
    private final CurrentUser currentUser;

    public CustomerController(CustomerService customerService, OrderService orderService,
                               CartService cartService, CurrentUser currentUser) {
        this.customerService = customerService;
        this.orderService = orderService;
        this.cartService = cartService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public ResponseEntity<CustomerProfileDto> getProfile() {
        return ResponseEntity.ok(customerService.getProfile(currentUser.id()));
    }

    @PutMapping
    public ResponseEntity<CustomerProfileDto> updateProfile(@Valid @RequestBody UpdateCustomerProfileRequestDto request) {
        return ResponseEntity.ok(customerService.updateProfile(currentUser.id(), request));
    }

    @GetMapping("/orders")
    public ResponseEntity<List<OrderDetailDto>> getOrderHistory() {
        return ResponseEntity.ok(orderService.getOrdersForCustomer(currentUser.id()));
    }

    @GetMapping("/cart")
    public ResponseEntity<List<CartItemDto>> getCart() {
        return ResponseEntity.ok(cartService.getCart(currentUser.id()));
    }

    @PostMapping("/cart")
    public ResponseEntity<CartItemDto> addToCart(@Valid @RequestBody CartItemRequestDto request) {
        return ResponseEntity.ok(cartService.addOrUpdateItem(currentUser.id(), request));
    }

    @PutMapping("/cart/{itemId}")
    public ResponseEntity<CartItemDto> updateCartItemQuantity(
            @PathVariable Long itemId, @Valid @RequestBody UpdateCartItemQuantityRequestDto request) {
        return ResponseEntity.ok(cartService.updateQuantity(currentUser.id(), itemId, request.quantity()));
    }

    @DeleteMapping("/cart/{itemId}")
    public ResponseEntity<Void> removeFromCart(@PathVariable Long itemId) {
        cartService.removeItem(currentUser.id(), itemId);
        return ResponseEntity.noContent().build();
    }
}
