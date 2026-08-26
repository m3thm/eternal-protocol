package com.eternalprotocol.api.command;

import com.eternalprotocol.api.dto.CartItemDto;
import com.eternalprotocol.api.exception.ResourceNotFoundException;
import com.eternalprotocol.api.service.CartService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartInvokerTest {

    @Mock
    private CartService cartService;

    @Test
    void executeAndTrack_runsTheCommand_andReturnsItsResult() {
        CartInvoker invoker = new CartInvoker(cartService);
        CartCommand command = mock(CartCommand.class);
        CartItemDto expected = new CartItemDto(100L, 10L, "Tee", new BigDecimal("4500"), "M", "Black", 1, "img.png");
        when(command.execute()).thenReturn(expected);

        CartItemDto result = invoker.executeAndTrack(1L, command);

        assertThat(result).isEqualTo(expected);
        verify(command).execute();
    }

    @Test
    void undoLast_noCommandsRunYetForThisCustomer_throwsResourceNotFoundException() {
        CartInvoker invoker = new CartInvoker(cartService);

        assertThatThrownBy(() -> invoker.undoLast(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void undoLast_undoesOnlyTheMostRecentCommand_notEarlierOnes() {
        CartInvoker invoker = new CartInvoker(cartService);
        CartCommand first = mock(CartCommand.class);
        CartCommand second = mock(CartCommand.class);

        invoker.executeAndTrack(1L, first);
        invoker.executeAndTrack(1L, second);
        invoker.undoLast(1L);

        // LIFO: the *second* command undone, the first left alone — same
        // "last action wins" behaviour as a typical Ctrl+Z stack.
        verify(second).undo();
        verify(first, never()).undo();
    }

    @Test
    void undoLast_calledTwice_undoesBothCommandsInReverseOrder() {
        CartInvoker invoker = new CartInvoker(cartService);
        CartCommand first = mock(CartCommand.class);
        CartCommand second = mock(CartCommand.class);

        invoker.executeAndTrack(1L, first);
        invoker.executeAndTrack(1L, second);
        invoker.undoLast(1L);
        invoker.undoLast(1L);

        verify(second).undo();
        verify(first).undo();
    }

    @Test
    void undoLast_afterHistoryExhausted_throwsResourceNotFoundException() {
        CartInvoker invoker = new CartInvoker(cartService);
        CartCommand only = mock(CartCommand.class);

        invoker.executeAndTrack(1L, only);
        invoker.undoLast(1L); // consumes the only recorded command

        assertThatThrownBy(() -> invoker.undoLast(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void undoLast_returnsTheCustomersCurrentCartAfterTheUndo() {
        CartInvoker invoker = new CartInvoker(cartService);
        CartCommand command = mock(CartCommand.class);
        List<CartItemDto> cartAfterUndo = List.of(
                new CartItemDto(100L, 10L, "Tee", new BigDecimal("4500"), "M", "Black", 1, "img.png"));
        when(cartService.getCart(1L)).thenReturn(cartAfterUndo);

        invoker.executeAndTrack(1L, command);
        List<CartItemDto> result = invoker.undoLast(1L);

        assertThat(result).isEqualTo(cartAfterUndo);
    }

    @Test
    void separateCustomers_haveIndependentUndoHistories() {
        CartInvoker invoker = new CartInvoker(cartService);
        CartCommand customerACommand = mock(CartCommand.class);

        invoker.executeAndTrack(1L, customerACommand); // only customer 1 has a history

        // Customer 2 never ran a command, so their undo must fail even
        // though customer 1's history is non-empty — histories must not
        // leak or share state across customers.
        assertThatThrownBy(() -> invoker.undoLast(2L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void historyBeyondCapacity_dropsTheOldestCommand() {
        CartInvoker invoker = new CartInvoker(cartService);
        // One more command than the invoker's documented per-customer cap (20).
        CartCommand oldest = mock(CartCommand.class);
        invoker.executeAndTrack(1L, oldest);
        for (int i = 0; i < 20; i++) {
            invoker.executeAndTrack(1L, mock(CartCommand.class));
        }

        // Undo everything that's still tracked (20 max) — the 21st undo
        // must fail because the oldest command was evicted to stay within
        // the cap, not because nothing was ever run.
        for (int i = 0; i < 20; i++) {
            invoker.undoLast(1L);
        }
        assertThatThrownBy(() -> invoker.undoLast(1L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(oldest, never()).undo();
    }
}
