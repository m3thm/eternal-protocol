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

    /**
     * Called right before deleting a Product: live carts referencing it
     * would otherwise block the delete with a foreign-key violation. Unlike
     * OrderItem, a CartItem is just in-progress shopper state — not
     * historical — so it's safe to clear rather than block on.
     */
    void deleteByProductId(Long productId);
}
