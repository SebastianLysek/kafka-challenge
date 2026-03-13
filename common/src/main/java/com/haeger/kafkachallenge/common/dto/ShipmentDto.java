package com.haeger.kafkachallenge.common.dto;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipmentDto {
    private Long id;
    private Long orderId;
    private Long customerId;
    private String customerEmail;
    private String customerFullName;
    private ShipmentStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant preparationStartedAt;
    private Instant shippedAt;
}
