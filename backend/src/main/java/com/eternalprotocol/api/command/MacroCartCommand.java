package com.eternalprotocol.api.command;

import com.eternalprotocol.api.dto.CartItemDto;

import java.util.ArrayList;
import java.util.List;

/**
 * A composite {@link CartCommand} — the "MacroCommand" this course's
 * material calls "party mode" functionality: several individual commands
 * bundled together and run (or undone) as a single unit, so
 * {@link CartInvoker} never needs to know whether it's tracking one cart
 * mutation or several.
 * <p>
 * The motivating case is "clear cart": rather than deleting every line
 * directly and losing per-line undo, {@code CartInvoker.clearCartAndTrack}
 * builds one {@link RemoveFromCartCommand} per current line and wraps them
 * all in a single {@code MacroCartCommand}. That gives "clear cart" the
 * same one-shot undo the rest of the cart already has, instead of a
 * customer having to undo N times to restore N lines.
 * <p>
 * {@link #undo()} reverses sub-commands in the opposite order they ran in
 * — the last one executed is the first one undone — the same LIFO
 * ordering {@link CartInvoker} itself uses for its per-customer history,
 * so an interrupted or partial macro always unwinds the same direction it
 * built up.
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
     * {@inheritDoc}
     * <p>
     * Always returns {@code null}: a macro touches multiple cart lines at
     * once, so no single {@link CartItemDto} can represent its result the
     * way a single command's does — a caller that needs the cart's new
     * state afterward should re-fetch it, which is exactly what
     * {@code CartInvoker.clearCartAndTrack} does.
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
     * Undoes every sub-command that actually ran, in reverse order.
     * <p>
     * Reversing only {@link #executed} (rather than all of
     * {@link #commands}) matters if a sub-command's own {@code execute()}
     * were to fail partway through the loop in {@link #execute()} — only
     * the ones that genuinely ran get undone, not ones that never had an
     * effect to reverse.
     */
    @Override
    public void undo() {
        for (int i = executed.size() - 1; i >= 0; i--) {
            executed.get(i).undo();
        }
    }
}
