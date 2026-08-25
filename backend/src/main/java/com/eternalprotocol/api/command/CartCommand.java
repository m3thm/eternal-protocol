package com.eternalprotocol.api.command;

import com.eternalprotocol.api.dto.CartItemDto;

/**
 * A single, self-contained cart mutation — the "Command" role in the
 * Command design pattern.
 * <p>
 * Each implementation wraps one {@code CartService} call (add, update
 * quantity, or remove) together with everything needed to reverse it, so
 * the invoker that runs a command never needs to know how to undo it —
 * only that it can. This is what decouples "what button the customer
 * clicked" ({@link com.eternalprotocol.api.controller.CustomerController})
 * from "how a cart line is actually changed"
 * ({@link com.eternalprotocol.api.service.CartService}): the controller
 * only ever creates a command and hands it to
 * {@link CartInvoker#executeAndTrack}, never calling {@code CartService}
 * directly for a mutating cart action.
 * <p>
 * {@link #execute()} must always be called exactly once, and always
 * before {@link #undo()} — calling {@code undo()} on a command that was
 * never executed is undefined and every implementation here treats it as
 * a no-op rather than throwing, since {@link CartInvoker} only ever calls
 * {@code undo()} on commands it has already executed itself.
 */
public interface CartCommand {

    /**
     * Performs the cart mutation and records whatever state is needed to
     * reverse it later.
     *
     * @return the cart line's state immediately after this command ran
     */
    CartItemDto execute();

    /**
     * Reverses exactly what {@link #execute()} did, restoring the cart
     * line (or the cart, for a removal) to its state beforehand.
     */
    void undo();
}
