package com.haeger.kafkachallenge.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDto {
    private Long id;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @NotNull
    private Integer productCategoryId;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private ProductCategoryDto category;

    @NotBlank
    private String sku;

    @NotBlank
    private String name;

    @NotNull
    @Positive
    private BigDecimal price;

    private String specs;
}
