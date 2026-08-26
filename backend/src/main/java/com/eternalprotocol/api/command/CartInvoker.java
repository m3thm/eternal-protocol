package com.eternalprotocol.api.command;

import com.eternalprotocol.api.dto.CartItemDto;
import com.eternalprotocol.api.exception.ResourceNotFoundException;
import com.eternalprotocol.api.service.CartService;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The "Invoker" role in the Command pattern: runs {@link CartCommand}s and
 * keeps a short per-customer history so the most recent one can be undone.
 * <p>
 * Every cart mutation goes through {@link #executeAndTrack} or
 * {@link #clearCartAndTrack}, and every undo through {@link #undoLast} —
 * this is the one place that decides which command is "most recent" for a
 * customer.
 * <p>
 * History is a stack-style {@link Deque}, one per customer, capped at
 * {@value #MAX_HISTORY_PER_CUSTOMER} entries. It's in-memory only and
 * resets on restart — an acceptable trade-off since it only needs to
 * survive one browsing session.
 * <p>
 * A Spring {@code @Component}, so its single shared instance comes from
 * Spring's container rather than a hand-written Singleton like
 * {@link com.eternalprotocol.api.cache.AthleteCodeCache}.
 */
@Component
public class CartInvoker {

    private static final int MAX_HISTORY_PER_CUSTOMER = 20;

    private final CartService cartService;

    /** One undo stack per customer, created on first use. */
    private final Map<Long, Deque<CartCommand>> historyByCustomer = new ConcurrentHashMap<>();

    /**
     * @param cartService needed to rebuild the cart's current state after an undo
     */
    public CartInvoker(CartService cartService) {
        this.cartService = cartService;
    }

    /**
     * Runs a cart command and records it as this customer's most recent
     * undoable action.
     *
     * @param customerId the customer the command acts on
     * @param command    the command to run
     * @return the command's own result — see {@link CartCommand#execute()}
     */
    public CartItemDto executeAndTrack(Long customerId, CartCommand command) {
        CartItemDto result = command.execute();
        track(customerId, command);
        return result;
    }

    /**
     * Clears a customer's entire cart as one undoable action: builds one
     * {@link RemoveFromCartCommand} per line, bundles them into a
     * {@link MacroCartCommand}, and tracks that macro like any other
     * command — so {@link #undoLast} restores every line in a single call.
     *
     * @param customerId the customer whose cart should be cleared
     * @return the customer's cart, now empty
     */
    public List<CartItemDto> clearCartAndTrack(Long customerId) {
        List<CartCommand> removals = cartService.getCart(customerId).stream()
                .<CartCommand>map(line -> new RemoveFromCartCommand(cartService, customerId, line.id()))
                .toList();

        MacroCartCommand macro = new MacroCartCommand(removals);
        macro.execute();
        track(customerId, macro);

        return cartService.getCart(customerId);
    }

    /**
     * Records a command as this customer's most recent undoable action,
     * evicting the oldest entry once the per-customer cap is exceeded.
     *
     * @param customerId the customer the command acts on
     * @param command    the already-executed command to record
     */
    private void track(Long customerId, CartCommand command) {
        Deque<CartCommand> history = historyByCustomer.computeIfAbsent(customerId, id -> new ArrayDeque<>());
        history.push(command);
        while (history.size() > MAX_HISTORY_PER_CUSTOMER) {
            history.removeLast();
        }
    }

    /**
     * Undoes the most recent cart command run for this customer.
     *
     * @param customerId the customer to undo the last action for
     * @return the customer's full cart, after the undo
     * @throws ResourceNotFoundException if this customer has no undoable action recorded
     */
    public List<CartItemDto> undoLast(Long customerId) {
        Deque<CartCommand> history = historyByCustomer.get(customerId);
        if (history == null || history.isEmpty()) {
            throw new ResourceNotFoundException("No recent cart action to undo");
        }

        CartCommand mostRecent = history.pop();
        mostRecent.undo();

        return cartService.getCart(customerId);
    }
}
