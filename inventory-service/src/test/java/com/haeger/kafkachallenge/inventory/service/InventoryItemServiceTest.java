package com.haeger.kafkachallenge.inventory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.haeger.kafkachallenge.common.dto.InventoryItemDto;
import com.haeger.kafkachallenge.inventory.entity.InventoryItem;
import com.haeger.kafkachallenge.inventory.repository.InventoryItemRepository;
import com.haeger.kafkachallenge.inventory.util.mapper.PojoMapper;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class InventoryItemServiceTest {

    @Mock
    private InventoryItemRepository inventoryItemRepository;

    @Mock
    private PojoMapper pojoMapper;

    @InjectMocks
    private InventoryItemService inventoryItemService;

    @Test
    void listInventoryItemsReturnsMappedDtos() {
        InventoryItem firstEntity = InventoryItem.builder().id(1L).quantity(5).build();
        InventoryItem secondEntity = InventoryItem.builder().id(2L).quantity(9).build();
        InventoryItemDto firstDto = InventoryItemDto.builder().id(1L).quantity(5).build();
        InventoryItemDto secondDto = InventoryItemDto.builder().id(2L).quantity(9).build();

        when(inventoryItemRepository.findAllBy()).thenReturn(List.of(firstEntity, secondEntity));
        when(pojoMapper.toInventoryItemDto(firstEntity)).thenReturn(firstDto);
        when(pojoMapper.toInventoryItemDto(secondEntity)).thenReturn(secondDto);

        List<InventoryItemDto> result = inventoryItemService.listInventoryItems();

        assertThat(result).containsExactly(firstDto, secondDto);
        verify(inventoryItemRepository).findAllBy();
        verify(pojoMapper).toInventoryItemDto(firstEntity);
        verify(pojoMapper).toInventoryItemDto(secondEntity);
    }

    @Test
    void createInventoryItemSavesMappedEntityAndReturnsDto() {
        InventoryItemDto requestDto = InventoryItemDto.builder().productId(10L).quantity(4).build();
        InventoryItem mappedEntity = new InventoryItem();
        InventoryItem savedEntity = InventoryItem.builder().id(7L).quantity(4).build();
        InventoryItemDto responseDto = InventoryItemDto.builder().id(7L).quantity(4).build();

        when(pojoMapper.toInventoryItemEntity(requestDto)).thenReturn(mappedEntity);
        when(inventoryItemRepository.save(mappedEntity)).thenReturn(savedEntity);
        when(pojoMapper.toInventoryItemDto(savedEntity)).thenReturn(responseDto);

        InventoryItemDto result = inventoryItemService.createInventoryItem(requestDto);

        assertThat(result).isSameAs(responseDto);
        verify(pojoMapper).toInventoryItemEntity(requestDto);
        verify(inventoryItemRepository).save(mappedEntity);
        verify(pojoMapper).toInventoryItemDto(savedEntity);
    }

    @Test
    void updateInventoryItemAppliesChangesAndReturnsDto() {
        Long inventoryItemId = 3L;
        InventoryItemDto requestDto = InventoryItemDto.builder().productId(12L).quantity(11).build();
        InventoryItem existingEntity = InventoryItem.builder().id(inventoryItemId).quantity(2).build();
        InventoryItem savedEntity = InventoryItem.builder().id(inventoryItemId).quantity(11).build();
        InventoryItemDto responseDto = InventoryItemDto.builder().id(inventoryItemId).quantity(11).build();

        when(inventoryItemRepository.findById(inventoryItemId)).thenReturn(Optional.of(existingEntity));
        when(inventoryItemRepository.save(existingEntity)).thenReturn(savedEntity);
        when(pojoMapper.toInventoryItemDto(savedEntity)).thenReturn(responseDto);

        InventoryItemDto result = inventoryItemService.updateInventoryItem(inventoryItemId, requestDto);

        assertThat(result).isSameAs(responseDto);
        verify(inventoryItemRepository).findById(inventoryItemId);
        verify(pojoMapper).applyInventoryItemWrite(requestDto, existingEntity);
        verify(inventoryItemRepository).save(existingEntity);
        verify(pojoMapper).toInventoryItemDto(savedEntity);
    }

    @Test
    void updateInventoryItemThrowsNotFoundWhenEntityDoesNotExist() {
        Long inventoryItemId = 99L;
        InventoryItemDto requestDto = InventoryItemDto.builder().productId(12L).quantity(11).build();

        when(inventoryItemRepository.findById(inventoryItemId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryItemService.updateInventoryItem(inventoryItemId, requestDto))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(exception -> {
                ResponseStatusException responseStatusException = (ResponseStatusException) exception;
                assertThat(responseStatusException.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                assertThat(responseStatusException.getReason()).isEqualTo("Inventory item not found");
            });

        verify(inventoryItemRepository).findById(inventoryItemId);
        verify(pojoMapper, never()).applyInventoryItemWrite(any(), any());
        verify(inventoryItemRepository, never()).save(any(InventoryItem.class));
    }
}
