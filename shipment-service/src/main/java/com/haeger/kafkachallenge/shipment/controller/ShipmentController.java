package com.haeger.kafkachallenge.shipment.controller;

import com.haeger.kafkachallenge.common.dto.ShipmentDto;
import com.haeger.kafkachallenge.shipment.service.ShipmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/shipments")
@RequiredArgsConstructor
@Tag(name = "Shipments", description = "Complete shipment preparation for an order.")
public class ShipmentController {
    private final ShipmentService shipmentService;

    @PostMapping("/{orderId}/complete")
    @Operation(summary = "Complete shipment", description = "Marks the shipment for the order as shipped and publishes an ORDER_SHIPPED event.")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Shipment completed successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ShipmentDto.class),
                examples = @ExampleObject(
                    name = "Completed shipment",
                    value = """
                        {
                          "id": 1,
                          "orderId": 1001,
                          "customerId": 42,
                          "customerEmail": "max@example.com",
                          "customerFullName": "Max Mustermann",
                          "status": "SHIPPED",
                          "createdAt": "2026-03-13T19:00:00Z",
                          "updatedAt": "2026-03-13T19:20:00Z",
                          "preparationStartedAt": "2026-03-13T19:05:00Z",
                          "shippedAt": "2026-03-13T19:20:00Z"
                        }
                        """
                )
            )
        ),
        @ApiResponse(responseCode = "404", description = "Shipment was not found for the given order"),
        @ApiResponse(responseCode = "409", description = "Shipment cannot be completed from its current status")
    })
    public ShipmentDto completeShipment(
        @Parameter(description = "Order identifier", example = "1001")
        @PathVariable Long orderId
    ) {
        return shipmentService.completeShipment(orderId);
    }
}
