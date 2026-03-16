package com.haeger.kafkachallenge.orders.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.haeger.kafkachallenge.common.dto.OrderDto;
import com.haeger.kafkachallenge.common.dto.OrderItemDto;
import com.haeger.kafkachallenge.common.dto.OrderStatus;
import com.haeger.kafkachallenge.orders.service.OrderService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({OrderController.class, CustomerOrderController.class})
class OrderControllerTest {
    private static final String TOO_LONG_256 = "x".repeat(256);


    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    @Test
    void createOrderReturnsCreatedOrder() throws Exception {
        OrderDto response = OrderDto.builder()
            .id(1001L)
            .customerId(42L)
            .customerEmail("max@example.com")
            .customerFullName("Max Mustermann")
            .status(OrderStatus.CREATED)
            .currency("EUR")
            .total(new BigDecimal("889.97"))
            .createdAt(Instant.parse("2026-03-08T19:00:00Z"))
            .updatedAt(Instant.parse("2026-03-08T19:00:00Z"))
            .items(new LinkedHashSet<>(List.of(
                OrderItemDto.builder()
                    .id(1L)
                    .orderId(1001L)
                    .productId(10L)
                    .productSku("CPU-AMD-7800X3D")
                    .productName("AMD Ryzen 7 7800X3D")
                    .unitPrice(new BigDecimal("399.99"))
                    .quantity(2)
                    .lineTotal(new BigDecimal("799.98"))
                    .build()
            )))
            .build();
        when(orderService.createOrder(any(OrderDto.class))).thenReturn(response);

        mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "customerId": 42,
                      "customerEmail": "max@example.com",
                      "customerFullName": "Max Mustermann",
                      "currency": "EUR",
                      "items": [
                        {
                          "productId": 10,
                          "quantity": 2
                        }
                      ]
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1001))
            .andExpect(jsonPath("$.status").value("CREATED"))
            .andExpect(jsonPath("$.items[0].productSku").value("CPU-AMD-7800X3D"));
    }

    @Test
    void createOrderRejectsPayloadThatExceedsEntityColumnLengths() throws Exception {
        mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(java.util.Map.of(
                    "customerId", 42,
                    "customerEmail", TOO_LONG_256 + "@example.com",
                    "customerFullName", TOO_LONG_256,
                    "currency", "EURO",
                    "notes", "ok",
                    "items", List.of(java.util.Map.of(
                        "productId", 10,
                        "quantity", 1
                    ))
                ))))
            .andExpect(status().isBadRequest());
    }

    @Test
    void listOrdersReturnsOrderCollection() throws Exception {
        when(orderService.listOrders()).thenReturn(List.of(sampleResponse(1001L, 42L)));

        mockMvc.perform(get("/orders"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(1001))
            .andExpect(jsonPath("$[0].customerId").value(42));
    }

    @Test
    void listOrdersByCustomerIdReturnsCustomerCollection() throws Exception {
        when(orderService.listOrdersByCustomerId(42L)).thenReturn(List.of(sampleResponse(1001L, 42L)));

        mockMvc.perform(get("/customers/42/orders"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(1001))
            .andExpect(jsonPath("$[0].customerId").value(42));
    }

    /**
     * Creates a sample OrderDto object with predefined values for testing purposes.
     *
     * @param orderId the unique identifier for the order
     * @param customerId the unique identifier for the customer associated with the order
     * @return an OrderDto object containing sample order details
     */
    private OrderDto sampleResponse(Long orderId, Long customerId) {
        return OrderDto.builder()
            .id(orderId)
            .customerId(customerId)
            .customerEmail("max@example.com")
            .customerFullName("Max Mustermann")
            .status(OrderStatus.CREATED)
            .currency("EUR")
            .total(new BigDecimal("889.97"))
            .createdAt(Instant.parse("2026-03-08T19:00:00Z"))
            .updatedAt(Instant.parse("2026-03-08T19:00:00Z"))
            .items(new LinkedHashSet<>(List.of(
                OrderItemDto.builder()
                    .id(1L)
                    .orderId(orderId)
                    .productId(10L)
                    .productSku("CPU-AMD-7800X3D")
                    .productName("AMD Ryzen 7 7800X3D")
                    .unitPrice(new BigDecimal("399.99"))
                    .quantity(2)
                    .lineTotal(new BigDecimal("799.98"))
                    .build()
            )))
            .build();
    }
}
