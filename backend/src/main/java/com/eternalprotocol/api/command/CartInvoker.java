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
 * {@link com.eternalprotocol.api.controller.CustomerController} never
 * calls {@code undo()} on a command directly — every single-line cart
 * mutation goes through {@link #executeAndTrack}, every whole-cart clear
 * goes through {@link #clearCartAndTrack} (built from a
 * {@link MacroCartCommand}), and every undo goes through
 * {@link #undoLast}, so this class is the single place that decides which
 * command is "the most recent one" for a given customer.
 * <p>
 * History is kept as a {@link Deque} used stack-style (push/pop from the
 * same end), one per customer, capped at {@value #MAX_HISTORY_PER_CUSTOMER}
 * entries so a long browsing session can't grow this map's memory use
 * without bound. This in-memory history is deliberately not persisted: it
 * only needs to survive one browsing session, and resetting on server
 * restart is an acceptable trade-off for the simplicity it buys.
 * <p>
 * Registered as a Spring {@code @Component} — meaning Spring's IoC
 * container manages this class's single, shared instance for the whole
 * running application (singleton scope by default). That is a different
 * mechanism from the classic Singleton pattern also covered in this
 * course: Spring reaches this behaviour through framework configuration,
 * not through a private constructor and a {@code getInstance()} method —
 * contrast with {@link com.eternalprotocol.api.cache.AthleteCodeCache},
 * which implements that classic pattern by hand.
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
     * Clears a customer's entire cart as one undoable action, using the
     * {@link MacroCartCommand} pattern instead of deleting every line
     * directly.
     * <p>
     * Builds one {@link RemoveFromCartCommand} per current cart line,
     * bundles them into a single {@code MacroCartCommand}, and tracks that
     * macro exactly like any other command — so {@link #undoLast} restores
     * every line the clear removed, in one call, rather than requiring the
     * customer to undo once per line.
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
     * Shared by {@link #executeAndTrack} and {@link #clearCartAndTrack} so
     * a single {@link MacroCartCommand} is tracked exactly like any other
     * {@link CartCommand} — {@link #undoLast} doesn't need to know or care
     * which kind it's popping.
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
