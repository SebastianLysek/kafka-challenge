package com.haeger.kafkachallenge.streaming.config;

import com.haeger.kafkachallenge.common.dto.InventoryItemDto;
import com.haeger.kafkachallenge.common.dto.OrderDto;
import com.haeger.kafkachallenge.common.dto.OrderItemDto;
import com.haeger.kafkachallenge.common.dto.ProductCategoryDto;
import com.haeger.kafkachallenge.common.dto.ProductDto;
import com.haeger.kafkachallenge.common.dto.ShipmentDto;
import com.haeger.kafkachallenge.streaming.inventory.entity.InventoryItem;
import com.haeger.kafkachallenge.streaming.inventory.entity.Product;
import com.haeger.kafkachallenge.streaming.inventory.entity.ProductCategory;
import com.haeger.kafkachallenge.streaming.order.entity.Order;
import com.haeger.kafkachallenge.streaming.order.entity.OrderItem;
import com.haeger.kafkachallenge.streaming.shipment.entity.Shipment;
import java.util.LinkedHashSet;
import org.springframework.stereotype.Component;

@Component
public class DtoMapper {
    public InventoryItemDto toInventoryItemDto(InventoryItem item) {
        return InventoryItemDto.builder()
            .id(item.getId())
            .productId(item.getProduct().getId())
            .product(toProductDto(item.getProduct()))
            .quantity(item.getQuantity())
            .build();
    }

    public ProductDto toProductDto(Product product) {
        return ProductDto.builder()
            .id(product.getId())
            .productCategoryId(product.getCategory().getId())
            .category(toProductCategoryDto(product.getCategory()))
            .sku(product.getSku())
            .name(product.getName())
            .price(product.getPrice())
            .specs(product.getSpecs())
            .build();
    }

    public ProductCategoryDto toProductCategoryDto(ProductCategory category) {
        return ProductCategoryDto.builder()
            .id(category.getId())
            .code(category.getCode())
            .name(category.getName())
            .sortOrder(category.getSortOrder())
            .build();
    }

    public OrderDto toOrderDto(Order order) {
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
                .map(this::toOrderItemDto)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new)))
            .build();
    }

    public OrderItemDto toOrderItemDto(OrderItem item) {
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

    public ShipmentDto toShipmentDto(Shipment shipment) {
        return ShipmentDto.builder()
            .id(shipment.getId())
            .orderId(shipment.getOrderId())
            .customerId(shipment.getCustomerId())
            .customerEmail(shipment.getCustomerEmail())
            .customerFullName(shipment.getCustomerFullName())
            .status(shipment.getStatus())
            .createdAt(shipment.getCreatedAt())
            .updatedAt(shipment.getUpdatedAt())
            .preparationStartedAt(shipment.getPreparationStartedAt())
            .shippedAt(shipment.getShippedAt())
            .build();
    }
}
