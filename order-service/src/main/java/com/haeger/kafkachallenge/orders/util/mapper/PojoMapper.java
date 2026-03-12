package com.haeger.kafkachallenge.orders.util.mapper;

import com.haeger.kafkachallenge.common.dto.OrderDto;
import com.haeger.kafkachallenge.common.dto.OrderItemDto;
import com.haeger.kafkachallenge.orders.entity.Order;
import com.haeger.kafkachallenge.orders.entity.OrderItem;
import java.util.LinkedHashSet;
import org.springframework.stereotype.Component;

@Component
public class PojoMapper {

    public OrderDto toOrderDto(Order entity) {
        if (entity == null) {
            return null;
        }
        return OrderDto.builder()
            .id(entity.getId())
            .customerId(entity.getCustomerId())
            .customerEmail(entity.getCustomerEmail())
            .customerFullName(entity.getCustomerFullName())
            .status(entity.getStatus())
            .currency(entity.getCurrency())
            .total(entity.getTotal())
            .notes(entity.getNotes())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .items(entity.getItems().stream()
                .map(this::toOrderItemDto)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new)))
            .build();
    }

    public OrderItemDto toOrderItemDto(OrderItem entity) {
        if (entity == null) {
            return null;
        }
        Long orderId = entity.getOrder() != null ? entity.getOrder().getId() : null;
        return OrderItemDto.builder()
            .id(entity.getId())
            .orderId(orderId)
            .productId(entity.getProductId())
            .productSku(entity.getProductSku())
            .productName(entity.getProductName())
            .unitPrice(entity.getUnitPrice())
            .quantity(entity.getQuantity())
            .lineTotal(entity.getLineTotal())
            .build();
    }

    public Order toOrderEntity(OrderDto dto) {
        if (dto == null) {
            return null;
        }
        Order entity = Order.builder()
            .id(dto.getId())
            .status(dto.getStatus())
            .total(dto.getTotal())
            .notes(dto.getNotes())
            .createdAt(dto.getCreatedAt())
            .updatedAt(dto.getUpdatedAt())
            .build();
        applyOrderWrite(dto, entity);
        if (dto.getItems() != null) {
            dto.getItems().stream()
                .map(this::toOrderItemEntity)
                .forEach(entity::addItem);
        }
        return entity;
    }

    public void applyOrderWrite(OrderDto dto, Order entity) {
        if (dto == null || entity == null) {
            return;
        }
        entity.setCustomerId(dto.getCustomerId());
        entity.setCustomerEmail(dto.getCustomerEmail());
        entity.setCustomerFullName(dto.getCustomerFullName());
        entity.setCurrency(dto.getCurrency());
        entity.setNotes(dto.getNotes());
    }

    public OrderItem toOrderItemEntity(OrderItemDto dto) {
        if (dto == null) {
            return null;
        }
        OrderItem entity = OrderItem.builder()
            .id(dto.getId())
            .productSku(dto.getProductSku())
            .productName(dto.getProductName())
            .unitPrice(dto.getUnitPrice())
            .lineTotal(dto.getLineTotal())
            .build();
        applyOrderItemWrite(dto, entity);
        return entity;
    }

    public void applyOrderItemWrite(OrderItemDto dto, OrderItem entity) {
        if (dto == null || entity == null) {
            return;
        }
        entity.setProductId(dto.getProductId());
        entity.setQuantity(dto.getQuantity());
    }
}
