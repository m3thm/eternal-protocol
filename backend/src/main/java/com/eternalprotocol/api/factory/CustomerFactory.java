package com.eternalprotocol.api.factory;

import com.eternalprotocol.api.dto.OrderRequestDto;
import com.eternalprotocol.api.entity.Customer;
import com.eternalprotocol.api.exception.ResourceNotFoundException;
import com.eternalprotocol.api.repository.CustomerRepository;
import org.springframework.stereotype.Component;

/**
 * Simple Factory that resolves the {@link Customer} a new order should be
 * attached to.
 * <p>
 * Checkout supports two different customers, decided purely from one
 * piece of input — whether the shopper is logged in:
 * <ul>
 *   <li>Logged in: reuse their existing account, refreshed with whatever
 *       delivery details were typed at checkout.</li>
 *   <li>Guest: create a brand-new, minimal {@code Customer} row just for
 *       this one order.</li>
 * </ul>
 * This is the same shape as the Simple Factory idiom from the Pizza Store
 * example — one method decides which construction path to take based on
 * its input, and hands back a finished object without the caller ever
 * needing to know which path was taken. There, the deciding input was a
 * pizza-type string switched over to pick a concrete {@code Pizza}
 * subclass; here it's the presence or absence of {@code loggedInCustomerId}
 * deciding between "update and reuse" versus "construct fresh" for the
 * same {@code Customer} type. Pulling this out of
 * {@link com.eternalprotocol.api.service.OrderService#createOrder} and
 * into its own class keeps that decision in exactly one place, so any
 * future third way to arrive at a checkout (e.g. a saved "buy again"
 * flow) only has to be taught to this factory, not to every caller.
 */
@Component
public class CustomerFactory {

    private final CustomerRepository customerRepository;

    /**
     * @param customerRepository access to customer data
     */
    public CustomerFactory(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    /**
     * Resolves the {@link Customer} a checkout should be attached to.
     *
     * @param loggedInCustomerId the current logged-in customer's ID, or null for a guest checkout
     * @param request            checkout details, used for the delivery info on either path
     * @return the customer to attach the new order to
     * @throws ResourceNotFoundException if {@code loggedInCustomerId} is supplied but doesn't match any account
     */
    public Customer resolveForCheckout(Long loggedInCustomerId, OrderRequestDto request) {
        if (loggedInCustomerId != null) {
            return updateExistingCustomer(loggedInCustomerId, request);
        }
        return createGuestCustomer(request);
    }

    /**
     * Refreshes an existing customer's delivery details with whatever was
     * submitted at checkout and saves the result.
     *
     * @param customerId the existing customer's ID
     * @param request    checkout details to copy onto the account
     * @return the updated, saved customer
     * @throws ResourceNotFoundException if no customer exists with this ID
     */
    private Customer updateExistingCustomer(Long customerId, OrderRequestDto request) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        // Keep delivery info current with whatever was submitted at checkout.
        customer.setName(request.customerName());
        customer.setPhone(request.phone());
        customer.setAddress(request.address());
        return customerRepository.save(customer);
    }

    /**
     * Creates and saves a brand-new customer row for a guest checkout,
     * mirroring how the storefront worked before customer accounts
     * existed.
     *
     * @param request checkout details to build the guest customer from
     * @return the newly created, saved customer
     */
    private Customer createGuestCustomer(OrderRequestDto request) {
        return customerRepository.save(
                new Customer(request.customerName(), request.phone(), request.address()));
    }
}
