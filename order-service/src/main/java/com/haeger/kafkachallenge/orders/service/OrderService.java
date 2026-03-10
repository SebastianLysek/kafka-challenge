package com.haeger.kafkachallenge.orders.service;

import com.haeger.kafkachallenge.common.dto.OrderDto;
import com.haeger.kafkachallenge.common.dto.OrderItemDto;
import com.haeger.kafkachallenge.common.dto.OrderStatus;
import com.haeger.kafkachallenge.common.events.EventTypes;
import com.haeger.kafkachallenge.common.events.IntegrationEvent;
import com.haeger.kafkachallenge.common.events.KafkaTopics;
import com.haeger.kafkachallenge.orders.entity.Order;
import com.haeger.kafkachallenge.orders.entity.OrderItem;
import com.haeger.kafkachallenge.orders.entity.ProductCatalogEntry;
import com.haeger.kafkachallenge.orders.repository.OrderRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final ProductCatalogProjectionService productCatalogProjectionService;
    private final OutboxService outboxService;

    @Transactional
    public OrderDto createOrder(OrderDto dto) {
        List<Long> productIds = dto.getItems().stream()
            .map(OrderItemDto::getProductId)
            .toList();
        Map<Long, ProductCatalogEntry> catalogEntries = productCatalogProjectionService.findByProductIds(productIds).stream()
            .collect(java.util.stream.Collectors.toMap(ProductCatalogEntry::getProductId, Function.identity()));
        Instant now = Instant.now();

        Order order = Order.builder()
            .customerId(dto.getCustomerId())
            .customerEmail(dto.getCustomerEmail())
            .customerFullName(dto.getCustomerFullName())
            .status(OrderStatus.CREATED)
            .currency(dto.getCurrency())
            .notes(dto.getNotes())
            .createdAt(now)
            .updatedAt(now)
            .total(BigDecimal.ZERO)
            .build();

        BigDecimal total = BigDecimal.ZERO;
        for (OrderItemDto itemDto : dto.getItems()) {
            ProductCatalogEntry product = catalogEntries.get(itemDto.getProductId());
            if (product == null) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unknown productId " + itemDto.getProductId() + " in order request"
                );
            }
            BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(itemDto.getQuantity()));
            total = total.add(lineTotal);

            order.addItem(OrderItem.builder()
                .productId(product.getProductId())
                .productSku(product.getSku())
                .productName(product.getName())
                .unitPrice(product.getPrice())
                .quantity(itemDto.getQuantity())
                .lineTotal(lineTotal)
                .build());
        }
        order.setTotal(total);

        Order savedOrder = orderRepository.save(order);
        OrderDto response = toDto(savedOrder);
        outboxService.enqueue(
            KafkaTopics.ORDERS,
            IntegrationEvent.of(EventTypes.ORDER_CREATED, savedOrder.getId().toString(), response)
        );
        return response;
    }

    @Transactional(readOnly = true)
    public List<OrderDto> listOrders() {
        return orderRepository.findAll().stream()
            .map(this::toDto)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderDto> listOrdersByCustomerId(Long customerId) {
        return orderRepository.findAllByCustomerId(customerId).stream()
            .map(this::toDto)
            .toList();
    }

    private OrderDto toDto(Order order) {
        return OrderDto.builder()
            .id(order.getId())
            .customerId(order.getCustomerId())
            .customerEmail(order.getCustomerEmail())
            .customerFullName(order.getCustomerFullName())
            .status(order.getStatus())
            .currency(order.getCurrency())
            .total(order.getTotal())
            .notes(order.getNotes())
            .createdAt(order.getCreatedAt())
            .updatedAt(order.getUpdatedAt())
            .items(order.getItems().stream()
                .map(this::toItemDto)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new)))
            .build();
    }

    private OrderItemDto toItemDto(OrderItem item) {
        return OrderItemDto.builder()
            .id(item.getId())
            .orderId(item.getOrder().getId())
            .productId(item.getProductId())
            .productSku(item.getProductSku())
            .productName(item.getProductName())
            .unitPrice(item.getUnitPrice())
            .quantity(item.getQuantity())
            .lineTotal(item.getLineTotal())
            .build();
    }
}
