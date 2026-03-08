package com.haeger.kafkachallenge.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductCategoryDto {
    private Integer id;

    @NotBlank
    private String code;

    @NotBlank
    private String name;

    @NotNull
    @Positive
    private Integer sortOrder;
}
