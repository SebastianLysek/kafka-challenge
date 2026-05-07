package com.haeger.kafkachallenge.streaming.inventory.service;

import com.haeger.kafkachallenge.common.dto.InventoryItemDto;
import com.haeger.kafkachallenge.streaming.config.DtoMapper;
import com.haeger.kafkachallenge.streaming.inventory.entity.InventoryItem;
import com.haeger.kafkachallenge.streaming.inventory.entity.Product;
import com.haeger.kafkachallenge.streaming.inventory.repository.InventoryItemRepository;
import com.haeger.kafkachallenge.streaming.inventory.repository.ProductRepository;
import com.haeger.kafkachallenge.streaming.streams.topology.ProtoEventPublisher;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class InventoryItemService {
    private final InventoryItemRepository inventoryItemRepository;
    private final ProductRepository productRepository;
    private final ProductEventFactory productEventFactory;
    private final ProtoEventPublisher eventPublisher;
    private final DtoMapper dtoMapper;

    @Transactional(readOnly = true)
    public List<InventoryItemDto> listInventoryItems() {
        return inventoryItemRepository.findAllBy().stream()
            .map(dtoMapper::toInventoryItemDto)
            .toList();
    }

    @Transactional
    public InventoryItemDto createInventoryItem(InventoryItemDto dto) {
        Product product = productRepository.findById(dto.getProductId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product not found"));
        InventoryItem saved = inventoryItemRepository.save(InventoryItem.builder()
            .product(product)
            .quantity(dto.getQuantity())
            .build());
        eventPublisher.publishProductUpserted(productEventFactory.toProductUpserted(saved));
        return dtoMapper.toInventoryItemDto(saved);
    }

    @Transactional
    public InventoryItemDto updateInventoryItem(Long id, InventoryItemDto dto) {
        InventoryItem entity = inventoryItemRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inventory item not found"));
        Product product = productRepository.findById(dto.getProductId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product not found"));
        entity.setProduct(product);
        entity.setQuantity(dto.getQuantity());
        InventoryItem saved = inventoryItemRepository.save(entity);
        eventPublisher.publishProductUpserted(productEventFactory.toProductUpserted(saved));
        return dtoMapper.toInventoryItemDto(saved);
    }
}
