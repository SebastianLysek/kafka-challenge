package com.haeger.kafkachallenge.shipment.util.mapper;

import com.haeger.kafkachallenge.common.dto.ShipmentDto;
import com.haeger.kafkachallenge.common.events.OrderCreatedPayload;
import com.haeger.kafkachallenge.shipment.entity.Shipment;
import org.springframework.stereotype.Component;

@Component
public class PojoMapper {

    /**
     * Maps the order data from the {@code OrderCreatedPayload} source object to the {@code Shipment} target object.
     * Copies relevant attributes such as order ID, customer ID, customer email, and customer full name from the source
     * to the target.
     *
     * @param source the source object containing order data, represented by {@code OrderCreatedPayload}
     * @param target the target object to which the order data will be mapped, represented by {@code Shipment}
     */
    public void applyOrderSnapshot(OrderCreatedPayload source, Shipment target) {
        target.setOrderId(source.getOrderId());
        target.setCustomerId(source.getCustomerId());
        target.setCustomerEmail(source.getCustomerEmail());
        target.setCustomerFullName(source.getCustomerFullName());
    }

    /**
     * Converts a {@link Shipment} entity to a {@link ShipmentDto}.
     * Maps all relevant fields from the {@code Shipment} entity to the {@code ShipmentDto}.
     *
     * @param shipment the {@code Shipment} entity to be converted
     * @return a {@code ShipmentDto} containing the mapped fields from the input {@code Shipment}
     */
    public ShipmentDto toShipmentDto(Shipment shipment) {
        return ShipmentDto.builder()
            .id(shipment.getId())
            .orderId(shipment.getOrderId())
            .customerId(shipment.getCustomerId())
            .customerEmail(shipment.getCustomerEmail())
            .customerFullName(shipment.getCustomerFullName())
            .status(shipment.getStatus())
            .createdAt(shipment.getCreatedAt())
            .updatedAt(shipment.getUpdatedAt())
            .preparationStartedAt(shipment.getPreparationStartedAt())
            .shippedAt(shipment.getShippedAt())
            .build();
    }
}
