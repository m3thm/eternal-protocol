package com.eternalprotocol.api.command;

import com.eternalprotocol.api.dto.CartItemDto;
import com.eternalprotocol.api.dto.CartItemRequestDto;
import com.eternalprotocol.api.service.CartService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AddToCartCommandTest {

    @Mock
    private CartService cartService;

    @Test
    void execute_noMatchingLineYet_addsAndReturnsResult() {
        CartItemRequestDto request = new CartItemRequestDto(10L, "M", "Black", 2);
        CartItemDto created = new CartItemDto(100L, 10L, "Tee", new BigDecimal("4500"), "M", "Black", 2, "img.png");

        when(cartService.getCart(1L)).thenReturn(List.of()); // no existing line
        when(cartService.addOrUpdateItem(1L, request)).thenReturn(created);

        AddToCartCommand command = new AddToCartCommand(cartService, 1L, request);
        CartItemDto result = command.execute();

        assertThat(result).isEqualTo(created);
    }

    @Test
    void undo_afterCreatingBrandNewLine_removesTheWholeLine() {
        CartItemRequestDto request = new CartItemRequestDto(10L, "M", "Black", 2);
        CartItemDto created = new CartItemDto(100L, 10L, "Tee", new BigDecimal("4500"), "M", "Black", 2, "img.png");

        when(cartService.getCart(1L)).thenReturn(List.of()); // no existing line before this add
        when(cartService.addOrUpdateItem(1L, request)).thenReturn(created);

        AddToCartCommand command = new AddToCartCommand(cartService, 1L, request);
        command.execute();
        command.undo();

        verify(cartService).removeItem(1L, 100L);
        verify(cartService, never()).updateQuantity(anyLong(), anyLong(), anyInt());
    }

    @Test
    void undo_afterIncrementingExistingLine_restoresThePreviousQuantity() {
        CartItemRequestDto request = new CartItemRequestDto(10L, "M", "Black", 3);
        CartItemDto existingBefore = new CartItemDto(100L, 10L, "Tee", new BigDecimal("4500"), "M", "Black", 1, "img.png");
        CartItemDto afterIncrement = new CartItemDto(100L, 10L, "Tee", new BigDecimal("4500"), "M", "Black", 4, "img.png");

        when(cartService.getCart(1L)).thenReturn(List.of(existingBefore)); // quantity 1 before this add
        when(cartService.addOrUpdateItem(1L, request)).thenReturn(afterIncrement);

        AddToCartCommand command = new AddToCartCommand(cartService, 1L, request);
        command.execute();
        command.undo();

        // Restores the original quantity (1), not zero and not a delete.
        verify(cartService).updateQuantity(1L, 100L, 1);
        verify(cartService, never()).removeItem(anyLong(), anyLong());
    }

    @Test
    void undo_calledBeforeExecute_doesNothing() {
        CartItemRequestDto request = new CartItemRequestDto(10L, "M", "Black", 1);
        AddToCartCommand command = new AddToCartCommand(cartService, 1L, request);

        command.undo();

        verifyNoInteractions(cartService);
    }
}
