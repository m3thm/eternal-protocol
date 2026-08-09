package com.eternalprotocol.api.service;

import com.eternalprotocol.api.dto.CustomerProfileDto;
import com.eternalprotocol.api.dto.UpdateCustomerProfileRequestDto;
import com.eternalprotocol.api.entity.Customer;
import com.eternalprotocol.api.exception.ResourceNotFoundException;
import com.eternalprotocol.api.repository.CustomerRepository;
import org.springframework.stereotype.Service;
@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public CustomerProfileDto getProfile(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        return toDto(customer);
    }

    public CustomerProfileDto updateProfile(Long customerId, UpdateCustomerProfileRequestDto request) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        customer.setName(request.name());
        customer.setPhone(request.phone());
        customer.setAddress(request.address());
        return toDto(customerRepository.save(customer));
    }

    private CustomerProfileDto toDto(Customer customer) {
        return new CustomerProfileDto(customer.getId(), customer.getName(), customer.getEmail(),
                customer.getPhone(), customer.getAddress());
    }
}
