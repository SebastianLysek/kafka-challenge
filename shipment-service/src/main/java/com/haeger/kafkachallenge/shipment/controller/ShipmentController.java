package com.haeger.kafkachallenge.shipment.controller;

import com.haeger.kafkachallenge.common.dto.ShipmentDto;
import com.haeger.kafkachallenge.shipment.service.ShipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/shipments")
@RequiredArgsConstructor
public class ShipmentController {
    private final ShipmentService shipmentService;

    @PostMapping("/{orderId}/complete")
    public ShipmentDto completeShipment(@PathVariable Long orderId) {
        return shipmentService.completeShipment(orderId);
    }
}
