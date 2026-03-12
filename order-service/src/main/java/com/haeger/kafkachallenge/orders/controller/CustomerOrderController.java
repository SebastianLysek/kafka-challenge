package com.haeger.kafkachallenge.orders.controller;

import com.haeger.kafkachallenge.common.dto.OrderDto;
import com.haeger.kafkachallenge.orders.service.OrderService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/customers/{customerId}/orders")
@RequiredArgsConstructor
public class CustomerOrderController {
    private final OrderService orderService;

    @GetMapping
    public List<OrderDto> listOrdersByCustomerId(@PathVariable Long customerId) {
        return orderService.listOrdersByCustomerId(customerId);
    }
}
