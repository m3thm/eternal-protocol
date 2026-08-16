package com.eternalprotocol.api.service;

import com.eternalprotocol.api.dto.CustomerProfileDto;
import com.eternalprotocol.api.dto.UpdateCustomerProfileRequestDto;
import com.eternalprotocol.api.entity.Customer;
import com.eternalprotocol.api.exception.ResourceNotFoundException;
import com.eternalprotocol.api.repository.CustomerRepository;
import org.springframework.stereotype.Service;

/**
 * Business logic for a logged-in customer's own profile.
 */
@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    /**
     * @param customerRepository access to customer data
     */
    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    /**
     * Gets a customer's profile.
     *
     * @param customerId customer's database ID
     * @return the customer's profile details
     * @throws ResourceNotFoundException if no customer exists with this ID
     */
    public CustomerProfileDto getProfile(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        return toDto(customer);
    }

    /**
     * Updates a customer's delivery details.
     *
     * @param customerId customer's database ID
     * @param request    updated name/phone/address
     * @return the updated profile
     * @throws ResourceNotFoundException if no customer exists with this ID
     */
    public CustomerProfileDto updateProfile(Long customerId, UpdateCustomerProfileRequestDto request) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        customer.setName(request.name());
        customer.setPhone(request.phone());
        customer.setAddress(request.address());
        return toDto(customerRepository.save(customer));
    }

    /**
     * Converts a database entity into its public-facing DTO shape.
     *
     * @param customer the entity to convert
     * @return the equivalent DTO
     */
    private CustomerProfileDto toDto(Customer customer) {
        return new CustomerProfileDto(customer.getId(), customer.getName(), customer.getEmail(),
                customer.getPhone(), customer.getAddress());
    }
}
