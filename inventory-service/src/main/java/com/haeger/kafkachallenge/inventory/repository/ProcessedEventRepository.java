package com.haeger.kafkachallenge.inventory.repository;

import com.haeger.kafkachallenge.inventory.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String> {
}
