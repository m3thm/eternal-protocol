package com.eternalprotocol.api.repository;

import com.eternalprotocol.api.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Data access for {@link CartItem} rows. Spring generates the
 * implementation of every method below automatically from its name.
 */
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    /**
     * Gets every cart line belonging to one customer.
     *
     * @param customerId customer's ID
     * @return that customer's cart lines
     */
    List<CartItem> findByCustomerId(Long customerId);

    /**
     * Looks up one cart line by its own ID, but only if it belongs to the
     * given customer — used so a customer can never edit another
     * customer's cart line.
     *
     * @param id         cart line ID
     * @param customerId owning customer's ID
     * @return the matching cart line, or empty if not found or not owned by this customer
     */
    Optional<CartItem> findByIdAndCustomerId(Long id, Long customerId);

    /**
     * Finds an existing cart line for the exact same product/size/colour
     * combination, so adding the same item twice increases quantity
     * instead of creating a duplicate row.
     *
     * @param customerId owning customer's ID
     * @param productId  product variant's ID
     * @param size       size
     * @param colour     colour
     * @return the matching cart line, or empty if none exists yet
     */
    Optional<CartItem> findByCustomerIdAndProductIdAndSizeAndColour(
            Long customerId, Long productId, String size, String colour);

    /**
     * Deletes every cart line belonging to one customer, e.g. after checkout.
     *
     * @param customerId owning customer's ID
     */
    void deleteByCustomerId(Long customerId);

    /**
     * Deletes every cart line referencing a product, called right before
     * that product is deleted. Live carts referencing a deleted product
     * would otherwise block the delete with a foreign-key error. Unlike an
     * {@code OrderItem}, a cart line is only in-progress shopper state, not
     * order history, so it is safe to remove instead of blocking the
     * delete.
     *
     * @param productId product variant's ID
     */
    void deleteByProductId(Long productId);
}
