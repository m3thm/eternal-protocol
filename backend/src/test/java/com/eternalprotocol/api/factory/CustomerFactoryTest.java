package com.eternalprotocol.api.factory;

import com.eternalprotocol.api.dto.OrderRequestDto;
import com.eternalprotocol.api.entity.Customer;
import com.eternalprotocol.api.exception.ResourceNotFoundException;
import com.eternalprotocol.api.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerFactoryTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerFactory customerFactory;

    private OrderRequestDto request(String name) {
        return new OrderRequestDto(name, "0771234567", "123 Main St", List.of(), null);
    }

    @Test
    void resolveForCheckout_noLoggedInId_createsABrandNewGuestCustomer() {
        OrderRequestDto request = request("Guest Shopper");
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> {
            Customer c = inv.getArgument(0);
            c.setId(9L);
            return c;
        });

        Customer result = customerFactory.resolveForCheckout(null, request);

        assertThat(result.getId()).isEqualTo(9L);
        assertThat(result.getName()).isEqualTo("Guest Shopper");
        verify(customerRepository, never()).findById(any());
    }

    @Test
    void resolveForCheckout_loggedInId_reusesAndUpdatesTheExistingAccount() {
        Customer existing = new Customer("Old Name", "0770000000", "Old Address");
        existing.setId(5L);
        OrderRequestDto request = request("New Name");

        when(customerRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        Customer result = customerFactory.resolveForCheckout(5L, request);

        // Same account (same ID), refreshed with the checkout's delivery details.
        assertThat(result.getId()).isEqualTo(5L);
        assertThat(result.getName()).isEqualTo("New Name");
        assertThat(result.getPhone()).isEqualTo("0771234567");
        assertThat(result.getAddress()).isEqualTo("123 Main St");
    }

    @Test
    void resolveForCheckout_loggedInIdDoesNotExist_throwsResourceNotFoundException() {
        when(customerRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerFactory.resolveForCheckout(404L, request("Anyone")))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(customerRepository, never()).save(any());
    }
}
