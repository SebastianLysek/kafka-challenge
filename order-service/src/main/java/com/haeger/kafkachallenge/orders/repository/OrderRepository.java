package com.haeger.kafkachallenge.orders.repository;

import com.haeger.kafkachallenge.orders.entity.Order;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
    @Override
    @EntityGraph(attributePaths = "items")
    List<Order> findAll();

    @EntityGraph(attributePaths = "items")
    List<Order> findAllByCustomerId(Long customerId);

    @Override
    @EntityGraph(attributePaths = "items")
    Optional<Order> findById(Long id);
}
