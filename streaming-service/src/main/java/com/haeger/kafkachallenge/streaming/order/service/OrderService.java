package com.haeger.kafkachallenge.streaming.order.service;

import com.haeger.kafkachallenge.common.dto.OrderDto;
import com.haeger.kafkachallenge.common.dto.OrderItemDto;
import com.haeger.kafkachallenge.common.dto.OrderStatus;
import com.haeger.kafkachallenge.streaming.config.DtoMapper;
import com.haeger.kafkachallenge.streaming.inventory.entity.InventoryItem;
import com.haeger.kafkachallenge.streaming.inventory.repository.InventoryItemRepository;
import com.haeger.kafkachallenge.streaming.order.entity.Order;
import com.haeger.kafkachallenge.streaming.order.entity.OrderItem;
import com.haeger.kafkachallenge.streaming.order.repository.OrderRepository;
import com.haeger.kafkachallenge.streaming.proto.NotificationRequested;
import com.haeger.kafkachallenge.streaming.proto.OrderCreated;
import com.haeger.kafkachallenge.streaming.proto.OrderCreatedItem;
import com.haeger.kafkachallenge.streaming.proto.OrderStatusChanged;
import com.haeger.kafkachallenge.streaming.shipment.service.ShipmentService;
import com.haeger.kafkachallenge.streaming.streams.topology.ProtoEventPublisher;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class OrderService {
    private static final Logger LOG = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final ProtoEventPublisher eventPublisher;
    private final DtoMapper dtoMapper;
    @Lazy
    private final ShipmentService shipmentService;

    @Transactional
    public OrderDto createOrder(OrderDto dto) {
        Map<Long, InventoryItem> inventoryByProductId = inventoryByProductIds(dto.getItems().stream()
            .map(OrderItemDto::getProductId)
            .toList());
        Instant now = Instant.now();

        Order order = Order.builder()
            .customerId(dto.getCustomerId())
            .customerEmail(dto.getCustomerEmail())
            .customerFullName(dto.getCustomerFullName())
            .status(OrderStatus.CREATED)
            .currency(dto.getCurrency())
            .notes(dto.getNotes())
            .total(BigDecimal.ZERO)
            .createdAt(now)
            .updatedAt(now)
            .build();

        BigDecimal total = BigDecimal.ZERO;
        for (OrderItemDto itemDto : dto.getItems()) {
            InventoryItem inventoryItem = inventoryByProductId.get(itemDto.getProductId());
            if (inventoryItem == null) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unknown productId " + itemDto.getProductId() + " in order request"
                );
            }
            BigDecimal lineTotal = inventoryItem.getProduct().getPrice().multiply(BigDecimal.valueOf(itemDto.getQuantity()));
            total = total.add(lineTotal);
            order.addItem(OrderItem.builder()
                .productId(inventoryItem.getProduct().getId())
                .productSku(inventoryItem.getProduct().getSku())
                .productName(inventoryItem.getProduct().getName())
                .unitPrice(inventoryItem.getProduct().getPrice())
                .quantity(itemDto.getQuantity())
                .lineTotal(lineTotal)
                .build());
        }

        order.setTotal(total);
        Order savedOrder = orderRepository.save(order);
        eventPublisher.publishNotificationRequested(toNotificationRequested(savedOrder, OrderStatus.CREATED));
        eventPublisher.publishOrderCreated(toOrderCreated(savedOrder));
        return dtoMapper.toOrderDto(savedOrder);
    }

    @Transactional(readOnly = true)
    public List<OrderDto> listOrders() {
        return orderRepository.findAll().stream()
            .map(dtoMapper::toOrderDto)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderDto> listOrdersByCustomerId(Long customerId) {
        return orderRepository.findAllByCustomerId(customerId).stream()
            .map(dtoMapper::toOrderDto)
            .toList();
    }

    @Transactional
    public void applyStatusChanged(OrderStatusChanged event) {
        OrderStatus targetStatus = OrderStatus.valueOf(event.getStatus());
        Order order = orderRepository.findById(event.getOrderId())
            .orElseThrow(() -> new IllegalStateException("Order %d not found".formatted(event.getOrderId())));

        if (!canTransition(order.getStatus(), targetStatus)) {
            LOG.info("Ignoring order {} transition from {} to {}", order.getId(), order.getStatus(), targetStatus);
            return;
        }

        order.setStatus(targetStatus);
        order.setUpdatedAt(Instant.now());
        Order savedOrder = orderRepository.save(order);
        eventPublisher.publishNotificationRequested(toNotificationRequested(savedOrder, targetStatus));

        if (targetStatus == OrderStatus.CONFIRMED) {
            shipmentService.startPreparation(savedOrder);
        }
    }

    @Transactional
    public void markPreparationStarted(Long orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalStateException("Order %d not found".formatted(orderId)));
        if (!canTransition(order.getStatus(), OrderStatus.PREP_STARTED)) {
            LOG.info("Ignoring order {} preparation transition from {}", orderId, order.getStatus());
            return;
        }
        order.setStatus(OrderStatus.PREP_STARTED);
        order.setUpdatedAt(Instant.now());
        Order savedOrder = orderRepository.save(order);
        eventPublisher.publishNotificationRequested(toNotificationRequested(savedOrder, OrderStatus.PREP_STARTED));
    }

    @Transactional(readOnly = true)
    public Order requireOrder(Long orderId) {
        return orderRepository.findById(orderId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
    }

    public NotificationRequested toNotificationRequested(Order order, OrderStatus status) {
        return NotificationRequested.newBuilder()
            .setOrderId(order.getId())
            .setCustomerId(order.getCustomerId())
            .setCustomerEmail(order.getCustomerEmail())
            .setCustomerFullName(order.getCustomerFullName())
            .setOrderStatus(status.name())
            .build();
    }

    private Map<Long, InventoryItem> inventoryByProductIds(Collection<Long> productIds) {
        return inventoryItemRepository.findAllByProduct_IdIn(productIds).stream()
            .collect(java.util.stream.Collectors.toMap(item -> item.getProduct().getId(), Function.identity()));
    }

    private OrderCreated toOrderCreated(Order order) {
        OrderCreated.Builder builder = OrderCreated.newBuilder()
            .setOrderId(order.getId())
            .setCustomerId(order.getCustomerId())
            .setCustomerEmail(order.getCustomerEmail())
            .setCustomerFullName(order.getCustomerFullName())
            .setCurrency(order.getCurrency());
        if (order.getNotes() != null) {
            builder.setNotes(order.getNotes());
        }
        order.getItems().forEach(item -> builder.addItems(OrderCreatedItem.newBuilder()
            .setProductId(item.getProductId())
            .setQuantity(item.getQuantity())
            .build()));
        return builder.build();
    }

    private boolean canTransition(OrderStatus currentStatus, OrderStatus targetStatus) {
        return switch (targetStatus) {
            case CONFIRMED, DECLINED -> currentStatus == OrderStatus.CREATED;
            case PREP_STARTED -> currentStatus == OrderStatus.CONFIRMED;
            case SHIPPED -> currentStatus == OrderStatus.PREP_STARTED;
            case CREATED -> false;
        };
    }
}
