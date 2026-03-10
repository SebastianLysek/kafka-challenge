package com.haeger.kafkachallenge.orders.repository;

import com.haeger.kafkachallenge.orders.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String> {
}
