package com.eternalprotocol.api.command;

import com.eternalprotocol.api.dto.CartItemDto;
import com.eternalprotocol.api.service.CartService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateQuantityCommandTest {

    @Mock
    private CartService cartService;

    @Test
    void execute_capturesPreviousQuantity_andSetsTheNewOne() {
        CartItemDto before = new CartItemDto(100L, 10L, "Tee", new BigDecimal("4500"), "M", "Black", 2, "img.png");
        CartItemDto after = new CartItemDto(100L, 10L, "Tee", new BigDecimal("4500"), "M", "Black", 5, "img.png");

        when(cartService.getCartItem(1L, 100L)).thenReturn(before);
        when(cartService.updateQuantity(1L, 100L, 5)).thenReturn(after);

        UpdateQuantityCommand command = new UpdateQuantityCommand(cartService, 1L, 100L, 5);
        CartItemDto result = command.execute();

        assertThat(result.quantity()).isEqualTo(5);
    }

    @Test
    void undo_restoresThePreviousQuantity_notTheNewOne() {
        CartItemDto before = new CartItemDto(100L, 10L, "Tee", new BigDecimal("4500"), "M", "Black", 2, "img.png");
        CartItemDto after = new CartItemDto(100L, 10L, "Tee", new BigDecimal("4500"), "M", "Black", 5, "img.png");

        when(cartService.getCartItem(1L, 100L)).thenReturn(before);
        when(cartService.updateQuantity(1L, 100L, 5)).thenReturn(after);

        UpdateQuantityCommand command = new UpdateQuantityCommand(cartService, 1L, 100L, 5);
        command.execute();
        command.undo();

        // Two distinct calls: one setting it to 5 (execute), one restoring it to 2 (undo).
        verify(cartService).updateQuantity(1L, 100L, 5);
        verify(cartService).updateQuantity(1L, 100L, 2);
    }

    @Test
    void undo_calledBeforeExecute_doesNothing() {
        UpdateQuantityCommand command = new UpdateQuantityCommand(cartService, 1L, 100L, 5);

        command.undo();

        verifyNoInteractions(cartService);
    }
}
