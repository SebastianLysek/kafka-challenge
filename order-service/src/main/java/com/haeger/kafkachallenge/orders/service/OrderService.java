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
import com.haeger.kafkachallenge.orders.util.mapper.PojoMapper;
import java.math.BigDecimal;
import java.time.Instant;
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
    private final PojoMapper pojoMapper;

    @Transactional
    public OrderDto createOrder(OrderDto dto) {
        List<Long> productIds = dto.getItems().stream()
            .map(OrderItemDto::getProductId)
            .toList();
        Map<Long, ProductCatalogEntry> catalogEntries = productCatalogProjectionService.findByProductIds(productIds).stream()
            .collect(java.util.stream.Collectors.toMap(ProductCatalogEntry::getProductId, Function.identity()));
        Instant now = Instant.now();

        Order order = new Order();
        pojoMapper.applyOrderWrite(dto, order);
        order.setStatus(OrderStatus.CREATED);
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        order.setTotal(BigDecimal.ZERO);

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

            OrderItem orderItem = pojoMapper.toOrderItemEntity(itemDto);
            orderItem.setProductSku(product.getSku());
            orderItem.setProductName(product.getName());
            orderItem.setUnitPrice(product.getPrice());
            orderItem.setLineTotal(lineTotal);
            order.addItem(orderItem);
        }
        order.setTotal(total);

        Order savedOrder = orderRepository.save(order);
        OrderDto response = pojoMapper.toOrderDto(savedOrder);
        outboxService.enqueue(
            KafkaTopics.ORDERS,
            IntegrationEvent.of(EventTypes.ORDER_CREATED, savedOrder.getId().toString(), response)
        );
        return response;
    }

    @Transactional(readOnly = true)
    public List<OrderDto> listOrders() {
        return orderRepository.findAll().stream()
            .map(pojoMapper::toOrderDto)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderDto> listOrdersByCustomerId(Long customerId) {
        return orderRepository.findAllByCustomerId(customerId).stream()
            .map(pojoMapper::toOrderDto)
            .toList();
    }
}
