package com.eternalprotocol.api.command;

import com.eternalprotocol.api.dto.CartItemDto;
import com.eternalprotocol.api.dto.CartItemRequestDto;
import com.eternalprotocol.api.service.CartService;

/**
 * Command for adding an item to a customer's cart.
 * <p>
 * {@link CartService#addOrUpdateItem} either creates a new line or
 * increases an existing one's quantity, so undo has two outcomes
 * depending on which happened — this class snapshots the cart before
 * calling {@code addOrUpdateItem} so {@link #undo()} knows which case applies.
 */
public class AddToCartCommand implements CartCommand {

    private final CartService cartService;
    private final Long customerId;
    private final CartItemRequestDto request;

    /** Quantity the matching line had before this command ran, or {@code null} if no matching line existed yet. */
    private Integer previousQuantity;

    /** The cart line's ID after {@link #execute()}, needed so {@link #undo()} knows which line to touch. */
    private Long affectedItemId;

    /**
     * @param cartService the cart logic this command wraps
     * @param customerId  the customer whose cart is being modified
     * @param request     the item to add
     */
    public AddToCartCommand(CartService cartService, Long customerId, CartItemRequestDto request) {
        this.cartService = cartService;
        this.customerId = customerId;
        this.request = request;
    }

    @Override
    public CartItemDto execute() {
        // null means no matching line exists yet — the signal undo() needs
        // to tell "incremented a line" apart from "created a new one".
        previousQuantity = cartService.getCart(customerId).stream()
                .filter(line -> line.productId().equals(request.productId())
                        && line.size().equals(request.size())
                        && line.colour().equals(request.colour()))
                .map(CartItemDto::quantity)
                .findFirst()
                .orElse(null);

        CartItemDto result = cartService.addOrUpdateItem(customerId, request);
        affectedItemId = result.id();
        return result;
    }

    @Override
    public void undo() {
        if (affectedItemId == null) {
            return; // execute() was never called
        }
        if (previousQuantity == null) {
            // This add created a brand-new line — undoing it means removing that line entirely.
            cartService.removeItem(customerId, affectedItemId);
        } else {
            // This add only incremented an existing line — undoing it means restoring the old quantity.
            cartService.updateQuantity(customerId, affectedItemId, previousQuantity);
        }
    }
}
