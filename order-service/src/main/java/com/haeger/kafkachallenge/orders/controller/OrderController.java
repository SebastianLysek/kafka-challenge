package com.haeger.kafkachallenge.orders.controller;

import com.haeger.kafkachallenge.common.dto.OrderDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.haeger.kafkachallenge.orders.service.OrderService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Create and inspect orders.")
public class OrderController {
    private final OrderService orderService;

    @GetMapping
    @Operation(summary = "List all orders", description = "Returns every order currently stored in the order service.")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Orders returned successfully",
            content = @Content(
                mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = OrderDto.class)),
                examples = @ExampleObject(
                    name = "Order list",
                    value = """
                        [
                          {
                            "id": 1001,
                            "customerId": 42,
                            "customerEmail": "max@example.com",
                            "customerFullName": "Max Mustermann",
                            "status": "CONFIRMED",
                            "currency": "EUR",
                            "total": 449.98,
                            "notes": "Please ring the bell twice.",
                            "createdAt": "2026-03-13T19:00:00Z",
                            "updatedAt": "2026-03-13T19:05:00Z",
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
    public List<OrderDto> listOrders() {
        return orderService.listOrders();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Create an order",
        description = "Creates a new order from the current product catalog snapshot and publishes an ORDER_CREATED event.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = OrderDto.class),
                examples = @ExampleObject(
                    name = "Create order request",
                    value = """
                        {
                          "customerId": 42,
                          "customerEmail": "max@example.com",
                          "customerFullName": "Max Mustermann",
                          "currency": "EUR",
                          "notes": "Please ring the bell twice.",
                          "items": [
                            {
                              "productId": 10,
                              "quantity": 1
                            },
                            {
                              "productId": 11,
                              "quantity": 2
                            }
                          ]
                        }
                        """
                )
            )
        )
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Order created successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = OrderDto.class),
                examples = @ExampleObject(
                    name = "Created order",
                    value = """
                        {
                          "id": 1001,
                          "customerId": 42,
                          "customerEmail": "max@example.com",
                          "customerFullName": "Max Mustermann",
                          "status": "CREATED",
                          "currency": "EUR",
                          "total": 1149.96,
                          "notes": "Please ring the bell twice.",
                          "createdAt": "2026-03-13T19:00:00Z",
                          "updatedAt": "2026-03-13T19:00:00Z",
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
                            },
                            {
                              "id": 2,
                              "orderId": 1001,
                              "productId": 11,
                              "productSku": "GPU-RTX-4070S",
                              "productName": "NVIDIA GeForce RTX 4070 SUPER",
                              "unitPrice": 349.99,
                              "quantity": 2,
                              "lineTotal": 699.98
                            }
                          ]
                        }
                        """
                )
            )
        ),
        @ApiResponse(responseCode = "400", description = "Order request referenced an unknown product")
    })
    public OrderDto createOrder(@Valid @RequestBody OrderDto dto) {
        return orderService.createOrder(dto);
    }
}
