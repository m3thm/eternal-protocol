package com.eternalprotocol.api.repository;

import com.eternalprotocol.api.entity.Order;
import com.eternalprotocol.api.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByCustomerIdOrderByOrderDateDesc(Long customerId);

    List<Order> findByPaymentStatus(OrderStatus paymentStatus);

    List<Order> findAllByOrderByOrderDateDesc();

    Optional<Order> findByPayhereOrderId(String payhereOrderId);
}
