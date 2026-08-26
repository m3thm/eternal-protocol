package com.eternalprotocol.api.command;

import com.eternalprotocol.api.dto.CartItemDto;

import java.util.ArrayList;
import java.util.List;

/**
 * A composite {@link CartCommand} (MacroCommand pattern): several
 * commands bundled together and run — or undone — as a single unit, so
 * {@link CartInvoker} doesn't need to know whether it's tracking one cart
 * mutation or several.
 * <p>
 * Used to clear a whole cart: {@code CartInvoker.clearCartAndTrack} builds
 * one {@link RemoveFromCartCommand} per line and wraps them all here,
 * giving "clear cart" the same one-shot undo as any other cart action.
 * <p>
 * {@link #undo()} reverses sub-commands in the opposite order they ran,
 * matching the LIFO ordering {@link CartInvoker} uses for its own history.
 */
public class MacroCartCommand implements CartCommand {

    private final List<CartCommand> commands;

    /** Sub-commands that have actually run, in run order, so {@link #undo()} can reverse exactly those. */
    private final List<CartCommand> executed = new ArrayList<>();

    /**
     * @param commands the sub-commands to run together, in the order they should execute
     */
    public MacroCartCommand(List<CartCommand> commands) {
        this.commands = List.copyOf(commands);
    }

    /**
     * Runs every sub-command in order.
     * <p>
     * Always returns {@code null}: a macro touches multiple cart lines at
     * once, so no single {@link CartItemDto} can represent its result — a
     * caller needing the cart's new state should re-fetch it.
     *
     * @return always {@code null}
     */
    @Override
    public CartItemDto execute() {
        for (CartCommand command : commands) {
            command.execute();
            executed.add(command);
        }
        return null;
    }

    /**
     * Undoes every sub-command that actually ran, in reverse order — only
     * {@link #executed}, so a partial failure mid-{@link #execute()} won't
     * try to undo commands that never ran.
     */
    @Override
    public void undo() {
        for (int i = executed.size() - 1; i >= 0; i--) {
            executed.get(i).undo();
        }
    }
}
