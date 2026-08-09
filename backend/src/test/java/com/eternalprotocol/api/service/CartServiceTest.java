package com.eternalprotocol.api.service;

import com.eternalprotocol.api.dto.CartItemDto;
import com.eternalprotocol.api.dto.CartItemRequestDto;
import com.eternalprotocol.api.entity.CartItem;
import com.eternalprotocol.api.entity.Customer;
import com.eternalprotocol.api.entity.Product;
import com.eternalprotocol.api.exception.ResourceNotFoundException;
import com.eternalprotocol.api.repository.CartItemRepository;
import com.eternalprotocol.api.repository.CustomerRepository;
import com.eternalprotocol.api.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CartService cartService;

    private Customer customer;
    private Product product;

    @BeforeEach
    void setUp() {
        customer = new Customer("Jane Doe", "0771234567", "123 Main St");
        customer.setId(1L);

        product = new Product();
        product.setId(10L);
        product.setName("Protocol 001 Oversized Tee");
        product.setPrice(new BigDecimal("4500.00"));
        product.setImageUrl("img.png");
    }

    @Test
    void addOrUpdateItem_newLine_createsWithRequestedQuantity() {
        CartItemRequestDto request = new CartItemRequestDto(10L, "M", "Black", 2);

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(cartItemRepository.findByCustomerIdAndProductIdAndSizeAndColour(1L, 10L, "M", "Black"))
                .thenReturn(Optional.empty());
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(inv -> {
            CartItem item = inv.getArgument(0);
            item.setId(100L);
            return item;
        });

        CartItemDto result = cartService.addOrUpdateItem(1L, request);

        assertThat(result.quantity()).isEqualTo(2);
        assertThat(result.productName()).isEqualTo("Protocol 001 Oversized Tee");
    }

    @Test
    void addOrUpdateItem_existingLine_incrementsQuantityInsteadOfDuplicating() {
        CartItem existing = new CartItem(customer, product, "M", "Black", 1);
        existing.setId(100L);
        CartItemRequestDto request = new CartItemRequestDto(10L, "M", "Black", 3);

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(cartItemRepository.findByCustomerIdAndProductIdAndSizeAndColour(1L, 10L, "M", "Black"))
                .thenReturn(Optional.of(existing));
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(inv -> inv.getArgument(0));

        CartItemDto result = cartService.addOrUpdateItem(1L, request);

        assertThat(result.quantity()).isEqualTo(4); // 1 existing + 3 new
        verify(cartItemRepository, times(1)).save(any(CartItem.class)); // one line, not a duplicate
    }

    @Test
    void addOrUpdateItem_unknownProduct_throwsResourceNotFoundException() {
        CartItemRequestDto request = new CartItemRequestDto(999L, "M", "Black", 1);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addOrUpdateItem(1L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void removeItem_belongsToCustomer_deletesIt() {
        CartItem item = new CartItem(customer, product, "M", "Black", 1);
        item.setId(100L);
        when(cartItemRepository.findByIdAndCustomerId(100L, 1L)).thenReturn(Optional.of(item));

        cartService.removeItem(1L, 100L);

        verify(cartItemRepository).delete(item);
    }

    @Test
    void removeItem_belongsToDifferentCustomer_throwsNotFound() {
        // findByIdAndCustomerId scopes the lookup to the calling customer,
        // so trying to remove someone else's item looks identical to it
        // not existing at all — this is what stops customer A from
        // deleting customer B's cart line by guessing an item id.
        when(cartItemRepository.findByIdAndCustomerId(100L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.removeItem(2L, 100L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getCart_returnsAllItemsForCustomer() {
        CartItem item = new CartItem(customer, product, "M", "Black", 1);
        item.setId(100L);
        when(cartItemRepository.findByCustomerId(1L)).thenReturn(java.util.List.of(item));

        var result = cartService.getCart(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).size()).isEqualTo("M");
    }

    @Test
    void clearCart_deletesAllItemsForCustomer() {
        cartService.clearCart(1L);

        verify(cartItemRepository).deleteByCustomerId(1L);
    }

    @Test
    void updateQuantity_ownItem_setsExactQuantity_notIncrement() {
        CartItem item = new CartItem(customer, product, "M", "Black", 1);
        item.setId(100L);
        when(cartItemRepository.findByIdAndCustomerId(100L, 1L)).thenReturn(Optional.of(item));
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(inv -> inv.getArgument(0));

        CartItemDto result = cartService.updateQuantity(1L, 100L, 5);

        assertThat(result.quantity()).isEqualTo(5); // set to 5, not 1+5
    }

    @Test
    void updateQuantity_anotherCustomersItem_throwsResourceNotFoundException() {
        when(cartItemRepository.findByIdAndCustomerId(100L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.updateQuantity(2L, 100L, 3))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
