package com.eternalprotocol.api.repository;

import com.eternalprotocol.api.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByCustomerId(Long customerId);

    Optional<CartItem> findByIdAndCustomerId(Long id, Long customerId);

    Optional<CartItem> findByCustomerIdAndProductIdAndSizeAndColour(
            Long customerId, Long productId, String size, String colour);

    void deleteByCustomerId(Long customerId);
}
