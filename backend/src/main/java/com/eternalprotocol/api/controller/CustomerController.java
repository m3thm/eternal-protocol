package com.eternalprotocol.api.controller;

import com.eternalprotocol.api.command.AddToCartCommand;
import com.eternalprotocol.api.command.CartCommand;
import com.eternalprotocol.api.command.CartInvoker;
import com.eternalprotocol.api.command.RemoveFromCartCommand;
import com.eternalprotocol.api.command.UpdateQuantityCommand;
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
 * REST endpoints for a logged-in customer's own account: profile, order
 * history, and saved cart.
 * <p>
 * Every endpoint here identifies "which customer" using
 * {@link CurrentUser}, which reads the ID from the verified JWT — never
 * from a path variable — so one customer can never view or edit another
 * customer's profile, orders, or cart just by changing a URL.
 * <p>
 * Every cart-mutating endpoint below builds a {@link CartCommand} and runs
 * it through {@link CartInvoker} instead of calling {@link CartService}
 * directly — see {@link CartInvoker} and the {@code command} package for
 * why (Command design pattern, with undo support).
 */
@RestController
@RequestMapping("/api/customers/me")
public class CustomerController {

    private final CustomerService customerService;
    private final OrderService orderService;
    private final CartService cartService;
    private final CartInvoker cartInvoker;
    private final CurrentUser currentUser;

    /**
     * @param customerService profile read/update logic
     * @param orderService    order history lookup
     * @param cartService     saved cart logic, for read-only cart access
     * @param cartInvoker     runs cart mutations as undoable commands
     * @param currentUser     resolves the logged-in customer's ID from the JWT
     */
    public CustomerController(CustomerService customerService, OrderService orderService,
                               CartService cartService, CartInvoker cartInvoker, CurrentUser currentUser) {
        this.customerService = customerService;
        this.orderService = orderService;
        this.cartService = cartService;
        this.cartInvoker = cartInvoker;
        this.currentUser = currentUser;
    }

    /**
     * Gets the logged-in customer's own profile.
     *
     * @return the customer's profile details
     */
    @GetMapping
    public ResponseEntity<CustomerProfileDto> getProfile() {
        return ResponseEntity.ok(customerService.getProfile(currentUser.id()));
    }

    /**
     * Updates the logged-in customer's own delivery details.
     *
     * @param request updated name/phone/address
     * @return the updated profile
     */
    @PutMapping
    public ResponseEntity<CustomerProfileDto> updateProfile(@Valid @RequestBody UpdateCustomerProfileRequestDto request) {
        return ResponseEntity.ok(customerService.updateProfile(currentUser.id(), request));
    }

    /**
     * Gets the logged-in customer's past orders.
     *
     * @return the customer's order history
     */
    @GetMapping("/orders")
    public ResponseEntity<List<OrderDetailDto>> getOrderHistory() {
        return ResponseEntity.ok(orderService.getOrdersForCustomer(currentUser.id()));
    }

    /**
     * Gets the logged-in customer's saved cart.
     *
     * @return the customer's current cart lines
     */
    @GetMapping("/cart")
    public ResponseEntity<List<CartItemDto>> getCart() {
        return ResponseEntity.ok(cartService.getCart(currentUser.id()));
    }

    /**
     * Adds an item to the logged-in customer's cart. If the exact same
     * product/size/colour is already in the cart, its quantity is
     * increased instead of creating a duplicate line. Runs as an
     * {@link AddToCartCommand} through {@link CartInvoker}, so this action
     * can be undone with {@link #undoLastCartAction}.
     *
     * @param request item to add
     * @return the added or updated cart line
     */
    @PostMapping("/cart")
    public ResponseEntity<CartItemDto> addToCart(@Valid @RequestBody CartItemRequestDto request) {
        CartCommand command = new AddToCartCommand(cartService, currentUser.id(), request);
        return ResponseEntity.ok(cartInvoker.executeAndTrack(currentUser.id(), command));
    }

    /**
     * Changes the quantity of one existing cart line. Runs as an
     * {@link UpdateQuantityCommand} through {@link CartInvoker}, so this
     * action can be undone with {@link #undoLastCartAction}.
     *
     * @param itemId  cart line's ID
     * @param request new quantity
     * @return the updated cart line
     */
    @PutMapping("/cart/{itemId}")
    public ResponseEntity<CartItemDto> updateCartItemQuantity(
            @PathVariable Long itemId, @Valid @RequestBody UpdateCartItemQuantityRequestDto request) {
        CartCommand command = new UpdateQuantityCommand(cartService, currentUser.id(), itemId, request.quantity());
        return ResponseEntity.ok(cartInvoker.executeAndTrack(currentUser.id(), command));
    }

    /**
     * Removes one line from the logged-in customer's cart. Runs as a
     * {@link RemoveFromCartCommand} through {@link CartInvoker}, so this
     * action can be undone with {@link #undoLastCartAction}.
     *
     * @param itemId cart line's ID
     * @return empty 204 response
     */
    @DeleteMapping("/cart/{itemId}")
    public ResponseEntity<Void> removeFromCart(@PathVariable Long itemId) {
        CartCommand command = new RemoveFromCartCommand(cartService, currentUser.id(), itemId);
        cartInvoker.executeAndTrack(currentUser.id(), command);
        return ResponseEntity.noContent().build();
    }

    /**
     * Undoes the most recent cart action (add, quantity change, or
     * removal) for the logged-in customer.
     *
     * @return the customer's cart, after the undo
     */
    @PostMapping("/cart/undo")
    public ResponseEntity<List<CartItemDto>> undoLastCartAction() {
        return ResponseEntity.ok(cartInvoker.undoLast(currentUser.id()));
    }
}
