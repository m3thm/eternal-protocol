package com.eternalprotocol.api.service;

import com.eternalprotocol.api.dto.CustomerProfileDto;
import com.eternalprotocol.api.entity.Customer;
import com.eternalprotocol.api.exception.ResourceNotFoundException;
import com.eternalprotocol.api.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerService customerService;

    @Test
    void getProfile_existingCustomer_returnsProfileDto() {
        Customer customer = new Customer("Jane Doe", "0771234567", "123 Main St");
        customer.setId(1L);
        customer.setEmail("jane@example.com");
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        CustomerProfileDto result = customerService.getProfile(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("Jane Doe");
        assertThat(result.email()).isEqualTo("jane@example.com");
    }

    @Test
    void getProfile_unknownCustomer_throwsResourceNotFoundException() {
        when(customerRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.getProfile(404L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateProfile_existingCustomer_updatesNamePhoneAddressOnly() {
        Customer customer = new Customer("Old Name", "0700000000", "Old Address");
        customer.setId(1L);
        customer.setEmail("jane@example.com"); // must survive the update untouched
        customer.setPasswordHash("hashed");    // must survive the update untouched

        com.eternalprotocol.api.dto.UpdateCustomerProfileRequestDto request =
                new com.eternalprotocol.api.dto.UpdateCustomerProfileRequestDto(
                        "New Name", "0771234567", "New Address");

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        CustomerProfileDto result = customerService.updateProfile(1L, request);

        assertThat(result.name()).isEqualTo("New Name");
        assertThat(result.phone()).isEqualTo("0771234567");
        assertThat(result.address()).isEqualTo("New Address");
        assertThat(result.email()).isEqualTo("jane@example.com"); // unchanged
    }

    @Test
    void updateProfile_unknownCustomer_throwsResourceNotFoundException() {
        com.eternalprotocol.api.dto.UpdateCustomerProfileRequestDto request =
                new com.eternalprotocol.api.dto.UpdateCustomerProfileRequestDto("Name", "0771234567", "Address");
        when(customerRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.updateProfile(404L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
