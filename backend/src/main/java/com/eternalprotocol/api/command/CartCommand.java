package com.eternalprotocol.api.command;

import com.eternalprotocol.api.dto.CartItemDto;

/**
 * A single, self-contained cart mutation (Command pattern). Each
 * implementation wraps one {@code CartService} call — add, update
 * quantity, or remove — together with everything needed to reverse it, so
 * {@link CartInvoker} can run and undo actions without knowing how each
 * one works.
 * <p>
 * {@link #execute()} must always be called exactly once, and before
 * {@link #undo()}. Calling {@code undo()} first is a no-op, not an error,
 * since {@link CartInvoker} only ever undoes commands it has already run.
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
