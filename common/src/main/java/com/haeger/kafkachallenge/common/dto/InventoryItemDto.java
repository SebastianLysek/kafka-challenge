package com.haeger.kafkachallenge.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryItemDto {
    private Long id;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @NotNull
    private Long productId;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private ProductDto product;

    @NotNull
    @PositiveOrZero
    private Integer quantity;
}
