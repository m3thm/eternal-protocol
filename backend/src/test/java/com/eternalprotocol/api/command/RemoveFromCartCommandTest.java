package com.eternalprotocol.api.command;

import com.eternalprotocol.api.dto.CartItemDto;
import com.eternalprotocol.api.dto.CartItemRequestDto;
import com.eternalprotocol.api.service.CartService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RemoveFromCartCommandTest {

    @Mock
    private CartService cartService;

    @Test
    void execute_removesTheLine_andReturnsASnapshotOfIt() {
        CartItemDto snapshot = new CartItemDto(100L, 10L, "Tee", new BigDecimal("4500"), "M", "Black", 3, "img.png");
        when(cartService.getCartItem(1L, 100L)).thenReturn(snapshot);

        RemoveFromCartCommand command = new RemoveFromCartCommand(cartService, 1L, 100L);
        CartItemDto result = command.execute();

        assertThat(result).isEqualTo(snapshot);
        verify(cartService).removeItem(1L, 100L);
    }

    @Test
    void undo_recreatesTheLineWithTheSameProductSizeColourAndQuantity() {
        CartItemDto snapshot = new CartItemDto(100L, 10L, "Tee", new BigDecimal("4500"), "M", "Black", 3, "img.png");
        when(cartService.getCartItem(1L, 100L)).thenReturn(snapshot);

        RemoveFromCartCommand command = new RemoveFromCartCommand(cartService, 1L, 100L);
        command.execute();
        command.undo();

        verify(cartService).addOrUpdateItem(1L, new CartItemRequestDto(10L, "M", "Black", 3));
    }

    @Test
    void undo_calledBeforeExecute_doesNothing() {
        RemoveFromCartCommand command = new RemoveFromCartCommand(cartService, 1L, 100L);

        command.undo();

        verifyNoInteractions(cartService);
    }
}
