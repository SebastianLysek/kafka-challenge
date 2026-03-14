package com.haeger.kafkachallenge.inventory.controller;

import com.haeger.kafkachallenge.common.dto.InventoryItemDto;
import com.haeger.kafkachallenge.inventory.service.InventoryItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Inventory", description = "Inspect and maintain the inventory stock.")
public class InventoryController {
    private final InventoryItemService inventoryItemService;

    @GetMapping
    @Operation(summary = "List inventory items", description = "Returns all inventory items including the referenced product details.")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Inventory returned successfully",
            content = @Content(
                mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = InventoryItemDto.class)),
                examples = @ExampleObject(
                    name = "Inventory list",
                    value = """
                        [
                          {
                            "id": 1,
                            "product": {
                              "id": 10,
                              "category": {
                                "id": 1,
                                "code": "CPU",
                                "name": "Processors",
                                "sortOrder": 10
                              },
                              "sku": "CPU-AMD-7800X3D",
                              "name": "AMD Ryzen 7 7800X3D",
                              "price": 449.98,
                              "specs": "8 cores / 16 threads"
                            },
                            "quantity": 25
                          }
                        ]
                        """
                )
            )
        )
    })
    public List<InventoryItemDto> listInventory() {
        return inventoryItemService.listInventoryItems();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Create an inventory item",
        description = "Creates a new inventory record for a product and emits a product upsert event.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = InventoryItemDto.class),
                examples = @ExampleObject(
                    name = "Create inventory item",
                    value = """
                        {
                          "productId": 10,
                          "quantity": 25
                        }
                        """
                )
            )
        )
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Inventory item created successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = InventoryItemDto.class),
                examples = @ExampleObject(
                    name = "Created inventory item",
                    value = """
                        {
                          "id": 1,
                          "product": {
                            "id": 10,
                            "category": {
                              "id": 1,
                              "code": "CPU",
                              "name": "Processors",
                              "sortOrder": 10
                            },
                            "sku": "CPU-AMD-7800X3D",
                            "name": "AMD Ryzen 7 7800X3D",
                            "price": 449.98,
                            "specs": "8 cores / 16 threads"
                          },
                          "quantity": 25
                        }
                        """
                )
            )
        )
    })
    public InventoryItemDto createInventoryItem(@Valid @RequestBody InventoryItemDto dto) {
        return inventoryItemService.createInventoryItem(dto);
    }

    @PutMapping("/{id}")
    @Operation(
        summary = "Update an inventory item",
        description = "Updates the available quantity of an existing inventory record and emits a product upsert event.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = InventoryItemDto.class),
                examples = @ExampleObject(
                    name = "Update inventory item",
                    value = """
                        {
                          "productId": 10,
                          "quantity": 18
                        }
                        """
                )
            )
        )
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Inventory item updated successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = InventoryItemDto.class),
                examples = @ExampleObject(
                    name = "Updated inventory item",
                    value = """
                        {
                          "id": 1,
                          "product": {
                            "id": 10,
                            "category": {
                              "id": 1,
                              "code": "CPU",
                              "name": "Processors",
                              "sortOrder": 10
                            },
                            "sku": "CPU-AMD-7800X3D",
                            "name": "AMD Ryzen 7 7800X3D",
                            "price": 449.98,
                            "specs": "8 cores / 16 threads"
                          },
                          "quantity": 18
                        }
                        """
                )
            )
        )
    })
    public InventoryItemDto updateInventoryItem(
        @Parameter(description = "Inventory item identifier", example = "1")
        @PathVariable Long id,
        @Valid @RequestBody InventoryItemDto dto
    ) {
        return inventoryItemService.updateInventoryItem(id, dto);
    }
}
