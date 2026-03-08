package com.haeger.kafkachallenge.inventory.service;

import com.haeger.kafkachallenge.common.dto.InventoryItemDto;
import com.haeger.kafkachallenge.inventory.entity.InventoryItem;
import com.haeger.kafkachallenge.inventory.repository.InventoryItemRepository;
import com.haeger.kafkachallenge.inventory.util.mapper.PojoMapper;
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
    private final PojoMapper pojoMapper;

    /**
     * Retrieves a list of inventory items along with their associated products and categories.
     * The resulting items are mapped to {@code InventoryItemDto} objects.
     *
     * @return a list of {@code InventoryItemDto} representing the inventory items with their product and category details.
     */
    @Transactional(readOnly = true)
    public List<InventoryItemDto> listInventoryItems() {
        return inventoryItemRepository.findAllBy().stream()
            .map(pojoMapper::toInventoryItemDto)
            .toList();
    }

    /**
     * Creates a new inventory item from the provided data transfer object (DTO).
     * The DTO is mapped to an entity, persisted in the database, and then converted
     * back to a DTO for the response.
     *
     * @param dto the {@code InventoryItemDto} containing data for the inventory item to be created
     * @return the {@code InventoryItemDto} representing the newly created inventory item
     */
    @Transactional
    public InventoryItemDto createInventoryItem(InventoryItemDto dto) {
        InventoryItem entity = pojoMapper.toInventoryItemEntity(dto);
        InventoryItem saved = inventoryItemRepository.save(entity);
        return pojoMapper.toInventoryItemDto(saved);
    }

    /**
     * Updates an existing inventory item identified by its ID with the provided data.
     * If the inventory item does not exist, an exception is thrown.
     *
     * @param id the ID of the inventory item to update
     * @param dto the {@code InventoryItemDto} containing the updated data for the inventory item
     * @return the updated {@code InventoryItemDto} representing the modified inventory item
     */
    @Transactional
    public InventoryItemDto updateInventoryItem(Long id, InventoryItemDto dto) {
        InventoryItem entity = inventoryItemRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inventory item not found"));
        pojoMapper.applyInventoryItemWrite(dto, entity);
        InventoryItem saved = inventoryItemRepository.save(entity);
        return pojoMapper.toInventoryItemDto(saved);
    }
}
