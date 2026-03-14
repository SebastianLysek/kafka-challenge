package com.haeger.kafkachallenge.orders.controller;

import com.haeger.kafkachallenge.common.dto.OrderDto;
import com.haeger.kafkachallenge.orders.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/customers/{customerId}/orders")
@RequiredArgsConstructor
@Tag(name = "Customer Orders", description = "Inspect orders for a specific customer.")
public class CustomerOrderController {
    private final OrderService orderService;

    @GetMapping
    @Operation(summary = "List orders by customer", description = "Returns all orders belonging to the provided customer identifier.")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Customer orders returned successfully",
            content = @Content(
                mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = OrderDto.class)),
                examples = @ExampleObject(
                    name = "Customer orders",
                    value = """
                        [
                          {
                            "id": 1001,
                            "customerId": 42,
                            "customerEmail": "max@example.com",
                            "customerFullName": "Max Mustermann",
                            "status": "SHIPPED",
                            "currency": "EUR",
                            "total": 449.98,
                            "notes": "Please ring the bell twice.",
                            "createdAt": "2026-03-13T19:00:00Z",
                            "updatedAt": "2026-03-13T19:20:00Z",
                            "items": [
                              {
                                "id": 1,
                                "orderId": 1001,
                                "productId": 10,
                                "productSku": "CPU-AMD-7800X3D",
                                "productName": "AMD Ryzen 7 7800X3D",
                                "unitPrice": 449.98,
                                "quantity": 1,
                                "lineTotal": 449.98
                              }
                            ]
                          }
                        ]
                        """
                )
            )
        )
    })
    public List<OrderDto> listOrdersByCustomerId(
        @Parameter(description = "Customer identifier", example = "42")
        @PathVariable Long customerId
    ) {
        return orderService.listOrdersByCustomerId(customerId);
    }
}
