package com.haeger.kafkachallenge.orders.service;

import com.haeger.kafkachallenge.common.dto.OrderDto;
import com.haeger.kafkachallenge.common.dto.OrderItemDto;
import com.haeger.kafkachallenge.common.dto.OrderStatus;
import com.haeger.kafkachallenge.common.events.EventTypes;
import com.haeger.kafkachallenge.common.events.IntegrationEvent;
import com.haeger.kafkachallenge.common.events.KafkaTopics;
import com.haeger.kafkachallenge.common.events.OrderCreatedItemPayload;
import com.haeger.kafkachallenge.common.events.OrderCreatedPayload;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class OrderService {
    private static final Logger LOG = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final ProductCatalogProjectionService productCatalogProjectionService;
    private final OutboxService outboxService;
    private final PojoMapper pojoMapper;

    /**
     * Creates a new order based on the provided OrderDto. The method processes the order items,
     * calculates the total cost, validates the product data, and persists the order. It also
     * enqueues an event to notify about the order creation.
     *
     * @param dto the data transfer object containing the details of the order to be created,
     *            including items, quantities, and other metadata
     * @return an OrderDto representing the created order, including its calculated total,
     *         current status, and additional metadata
     * @throws ResponseStatusException if any of the productIds in the order items cannot be found
     */
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
            IntegrationEvent.of(EventTypes.ORDER_CREATED, savedOrder.getId().toString(), toOrderCreatedPayload(savedOrder))
        );
        return response;
    }

    /**
     * Retrieves a list of all orders and maps them to OrderDto objects.
     *
     * @return a list of OrderDto representing all orders in the repository
     */
    @Transactional(readOnly = true)
    public List<OrderDto> listOrders() {
        return orderRepository.findAll().stream()
            .map(pojoMapper::toOrderDto)
            .toList();
    }

    /**
     * Retrieves a list of orders associated with the given customer ID.
     * This method is marked as read-only transactional.
     *
     * @param customerId the ID of the customer whose orders are to be retrieved
     * @return a list of OrderDto objects representing the customer's orders
     */
    @Transactional(readOnly = true)
    public List<OrderDto> listOrdersByCustomerId(Long customerId) {
        return orderRepository.findAllByCustomerId(customerId).stream()
            .map(pojoMapper::toOrderDto)
            .toList();
    }

    /**
     * Confirms an order by transitioning its status from CREATED to CONFIRMED
     * and enqueues an integration event for the confirmation.
     *
     * @param orderId the unique identifier of the order to be confirmed
     */
    @Transactional
    public void confirmOrder(Long orderId) {
        Order savedOrder = transitionOrderStatus(orderId, OrderStatus.CREATED, OrderStatus.CONFIRMED);
        if (savedOrder == null) {
            return;
        }

        outboxService.enqueue(
            KafkaTopics.ORDERS,
            IntegrationEvent.of(
                EventTypes.ORDER_CONFIRMED,
                savedOrder.getId().toString(),
                toOrderCreatedPayload(savedOrder)
            )
        );
    }

    /**
     * Declines an order by updating its status to 'DECLINED' and enqueues
     * an integration event to notify other services about the status change.
     *
     * @param orderId the unique identifier of the order to be declined
     */
    @Transactional
    public void declineOrder(Long orderId) {
        Order savedOrder = transitionOrderStatus(orderId, OrderStatus.CREATED, OrderStatus.DECLINED);
        if (savedOrder == null) {
            return;
        }

        outboxService.enqueue(
            KafkaTopics.ORDERS,
            IntegrationEvent.of(
                EventTypes.ORDER_DECLINED,
                savedOrder.getId().toString(),
                toOrderCreatedPayload(savedOrder)
            )
        );
    }

    /**
     * Initiates the shipment preparation process for the specified order.
     * Changes the status of the order from "CONFIRMED" to "PREP_STARTED".
     *
     * @param orderId the unique identifier of the order that will enter the shipment preparation phase
     */
    @Transactional
    public void startShipmentPreparation(Long orderId) {
        transitionOrderStatus(orderId, OrderStatus.CONFIRMED, OrderStatus.PREP_STARTED);
    }

    /**
     * Marks the specified order as shipped by transitioning its status from
     * PREP_STARTED to SHIPPED.
     *
     * @param orderId the unique identifier of the order to be marked as shipped
     */
    @Transactional
    public void markShipped(Long orderId) {
        transitionOrderStatus(orderId, OrderStatus.PREP_STARTED, OrderStatus.SHIPPED);
    }

    /**
     * Transitions the status of an order to a target status if the current status matches the expected status.
     *
     * @param orderId The unique identifier of the order to be updated.
     * @param expectedCurrentStatus The expected current status of the order. If the actual status of the order
     *        does not match this status, the transition will not occur.
     * @param targetStatus The target status to which the order's status should be updated.
     * @return The updated {@code Order} object if the transition is successful. Returns {@code null} if the
     *         order's current status does not match the expected status or if the target status is the same as
     *         the current status.
     * @throws IllegalStateException If the order with the given {@code orderId} is not found.
     */
    private Order transitionOrderStatus(Long orderId, OrderStatus expectedCurrentStatus, OrderStatus targetStatus) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalStateException("Order %d not found".formatted(orderId)));

        if (order.getStatus() == targetStatus) {
            LOG.info("Order {} already has status {}, skipping", orderId, targetStatus);
            return null;
        }
        if (order.getStatus() != expectedCurrentStatus) {
            LOG.warn("Ignoring transition of order {} from {} to {}", orderId, order.getStatus(), targetStatus);
            return null;
        }

        order.setStatus(targetStatus);
        order.setUpdatedAt(Instant.now());

        return orderRepository.save(order);
    }

    /**
     * Converts an Order object to an OrderCreatedPayload object.
     *
     * @param order the Order object to be converted
     * @return an OrderCreatedPayload object containing the corresponding data from the given Order object
     */
    private OrderCreatedPayload toOrderCreatedPayload(Order order) {
        return OrderCreatedPayload.builder()
            .orderId(order.getId())
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
                .map(item -> OrderCreatedItemPayload.builder()
                    .productId(item.getProductId())
                    .productSku(item.getProductSku())
                    .productName(item.getProductName())
                    .unitPrice(item.getUnitPrice())
                    .quantity(item.getQuantity())
                    .lineTotal(item.getLineTotal())
                    .build())
                .toList())
            .build();
    }
}
