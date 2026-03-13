package com.haeger.kafkachallenge.shipment.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.haeger.kafkachallenge.common.dto.ShipmentDto;
import com.haeger.kafkachallenge.common.dto.ShipmentStatus;
import com.haeger.kafkachallenge.shipment.service.ShipmentService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ShipmentController.class)
class ShipmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ShipmentService shipmentService;

    @Test
    void completeShipmentReturnsUpdatedShipment() throws Exception {
        when(shipmentService.completeShipment(1001L)).thenReturn(ShipmentDto.builder()
            .id(501L)
            .orderId(1001L)
            .customerId(42L)
            .customerEmail("max@example.com")
            .customerFullName("Max Mustermann")
            .status(ShipmentStatus.SHIPPED)
            .createdAt(Instant.parse("2026-03-12T18:00:00Z"))
            .updatedAt(Instant.parse("2026-03-12T18:30:00Z"))
            .preparationStartedAt(Instant.parse("2026-03-12T18:00:00Z"))
            .shippedAt(Instant.parse("2026-03-12T18:30:00Z"))
            .build());

        mockMvc.perform(post("/shipments/1001/complete"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderId").value(1001))
            .andExpect(jsonPath("$.status").value("SHIPPED"));
    }
}
