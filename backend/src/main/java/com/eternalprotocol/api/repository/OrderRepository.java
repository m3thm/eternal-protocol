package com.eternalprotocol.api.repository;

import com.eternalprotocol.api.entity.Order;
import com.eternalprotocol.api.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Data access for {@link Order} rows. Spring generates the implementation
 * of every method below automatically from its name.
 */
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * Gets one customer's order history, most recent first.
     *
     * @param customerId customer's ID
     * @return that customer's orders, newest first
     */
    List<Order> findByCustomerIdOrderByOrderDateDesc(Long customerId);

    /**
     * Gets every order currently in a given payment status.
     *
     * @param paymentStatus status to filter by
     * @return matching orders
     */
    List<Order> findByPaymentStatus(OrderStatus paymentStatus);

    /**
     * Gets every order in the system, most recent first — used by the
     * admin dashboard's order list.
     *
     * @return all orders, newest first
     */
    List<Order> findAllByOrderByOrderDateDesc();

    /**
     * Looks up an order by the reference sent to PayHere, used when
     * PayHere calls back to confirm a payment.
     *
     * @param payhereOrderId this app's own order reference
     * @return the matching order, or empty if none found
     */
    Optional<Order> findByPayhereOrderId(String payhereOrderId);
}
