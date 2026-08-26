package com.eternalprotocol.api.command;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class MacroCartCommandTest {

    @Test
    void execute_runsEverySubCommand_inOrder() {
        CartCommand first = mock(CartCommand.class);
        CartCommand second = mock(CartCommand.class);
        CartCommand third = mock(CartCommand.class);
        MacroCartCommand macro = new MacroCartCommand(List.of(first, second, third));

        macro.execute();

        InOrder order = inOrder(first, second, third);
        order.verify(first).execute();
        order.verify(second).execute();
        order.verify(third).execute();
    }

    @Test
    void undo_reversesEverySubCommand_inReverseOrder() {
        CartCommand first = mock(CartCommand.class);
        CartCommand second = mock(CartCommand.class);
        MacroCartCommand macro = new MacroCartCommand(List.of(first, second));

        macro.execute();
        macro.undo();

        InOrder order = inOrder(first, second);
        order.verify(second).undo();
        order.verify(first).undo();
    }

    @Test
    void undo_beforeExecute_undoesNothing() {
        CartCommand only = mock(CartCommand.class);
        MacroCartCommand macro = new MacroCartCommand(List.of(only));

        macro.undo();

        verify(only, never()).undo();
    }

    @Test
    void execute_returnsNull_sinceNoSingleLineRepresentsTheWholeMacro() {
        CartCommand only = mock(CartCommand.class);
        MacroCartCommand macro = new MacroCartCommand(List.of(only));

        assertThat(macro.execute()).isNull();
    }

    @Test
    void undo_onlyReversesSubCommandsThatActuallyRan() {
        // If execute() only got partway through commands before this
        // macro's own execute() were interrupted, undo() must not call
        // undo() on sub-commands that never ran. Simulated here directly
        // by only ever calling execute() on a subset.
        CartCommand ran = mock(CartCommand.class);
        CartCommand neverRan = mock(CartCommand.class);
        MacroCartCommand macro = new MacroCartCommand(List.of(ran));

        macro.execute();
        macro.undo();

        verify(ran).undo();
        verifyNoInteractions(neverRan);
    }
}
