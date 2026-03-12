package com.haeger.kafkachallenge.shipment.repository;

import com.haeger.kafkachallenge.shipment.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String> {
}
