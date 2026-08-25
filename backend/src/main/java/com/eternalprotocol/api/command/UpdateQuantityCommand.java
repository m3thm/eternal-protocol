package com.eternalprotocol.api.command;

import com.eternalprotocol.api.dto.CartItemDto;
import com.eternalprotocol.api.service.CartService;

/**
 * Command for setting an existing cart line to an exact quantity — what a
 * "+"/"-" stepper on the cart page should build and hand to
 * {@link CartInvoker}.
 */
public class UpdateQuantityCommand implements CartCommand {

    private final CartService cartService;
    private final Long customerId;
    private final Long itemId;
    private final int newQuantity;

    /** The line's quantity before this command ran, captured in {@link #execute()} so {@link #undo()} can restore it. */
    private Integer previousQuantity;

    /**
     * @param cartService the cart logic this command wraps
     * @param customerId  the customer whose cart line is being changed
     * @param itemId      the cart line's ID
     * @param newQuantity the quantity to set
     */
    public UpdateQuantityCommand(CartService cartService, Long customerId, Long itemId, int newQuantity) {
        this.cartService = cartService;
        this.customerId = customerId;
        this.itemId = itemId;
        this.newQuantity = newQuantity;
    }

    @Override
    public CartItemDto execute() {
        previousQuantity = cartService.getCartItem(customerId, itemId).quantity();
        return cartService.updateQuantity(customerId, itemId, newQuantity);
    }

    @Override
    public void undo() {
        if (previousQuantity == null) {
            return; // execute() was never called
        }
        cartService.updateQuantity(customerId, itemId, previousQuantity);
    }
}
