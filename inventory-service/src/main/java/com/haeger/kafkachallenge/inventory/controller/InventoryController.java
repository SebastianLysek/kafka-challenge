package com.haeger.kafkachallenge.inventory.controller;

import com.haeger.kafkachallenge.common.dto.InventoryItemDto;
import com.haeger.kafkachallenge.inventory.service.InventoryItemService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {
    private final InventoryItemService inventoryItemService;

    @GetMapping
    public List<InventoryItemDto> listInventory() {
        return inventoryItemService.listInventoryItems();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryItemDto createInventoryItem(@Valid @RequestBody InventoryItemDto dto) {
        return inventoryItemService.createInventoryItem(dto);
    }

    @PutMapping("/{id}")
    public InventoryItemDto updateInventoryItem(@PathVariable Long id, @Valid @RequestBody InventoryItemDto dto) {
        return inventoryItemService.updateInventoryItem(id, dto);
    }
}
