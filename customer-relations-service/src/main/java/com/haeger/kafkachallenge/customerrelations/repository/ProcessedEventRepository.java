package com.haeger.kafkachallenge.customerrelations.repository;

import com.haeger.kafkachallenge.customerrelations.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String> {
}
