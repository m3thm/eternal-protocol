package com.eternalprotocol.api.command;

import com.eternalprotocol.api.dto.CartItemDto;
import com.eternalprotocol.api.dto.CartItemRequestDto;
import com.eternalprotocol.api.service.CartService;

/**
 * Command for removing one line from a customer's cart.
 * <p>
 * Unlike {@link AddToCartCommand} and {@link UpdateQuantityCommand}, this
 * command's {@link #execute()} does not return the mutated line's new
 * state (there isn't one — it's gone); it returns a snapshot of the line
 * exactly as it stood right before removal, so a caller such as the
 * "Removed — Undo?" toast on the cart page can still show what was just
 * taken out.
 */
public class RemoveFromCartCommand implements CartCommand {

    private final CartService cartService;
    private final Long customerId;
    private final Long itemId;

    /** Full snapshot of the line just before it was deleted, needed to recreate it on {@link #undo()}. */
    private CartItemDto removedSnapshot;

    /**
     * @param cartService the cart logic this command wraps
     * @param customerId  the customer whose cart line is being removed
     * @param itemId      the cart line's ID
     */
    public RemoveFromCartCommand(CartService cartService, Long customerId, Long itemId) {
        this.cartService = cartService;
        this.customerId = customerId;
        this.itemId = itemId;
    }

    @Override
    public CartItemDto execute() {
        removedSnapshot = cartService.getCartItem(customerId, itemId);
        cartService.removeItem(customerId, itemId);
        return removedSnapshot;
    }

    @Override
    public void undo() {
        if (removedSnapshot == null) {
            return; // execute() was never called
        }
        // Recreates the line via the normal add path, so it gets a fresh
        // ID rather than the deleted row's original one — fine for a cart.
        cartService.addOrUpdateItem(customerId, new CartItemRequestDto(
                removedSnapshot.productId(), removedSnapshot.size(),
                removedSnapshot.colour(), removedSnapshot.quantity()));
    }
}
