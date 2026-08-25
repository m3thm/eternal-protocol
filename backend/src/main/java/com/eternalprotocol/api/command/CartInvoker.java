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
 * calls {@code undo()} on a command directly — every cart mutation goes
 * through {@link #executeAndTrack}, and every undo goes through
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
 * not through a private constructor and a {@code getInstance()} method.
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

        Deque<CartCommand> history = historyByCustomer.computeIfAbsent(customerId, id -> new ArrayDeque<>());
        history.push(command);
        while (history.size() > MAX_HISTORY_PER_CUSTOMER) {
            history.removeLast();
        }

        return result;
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
