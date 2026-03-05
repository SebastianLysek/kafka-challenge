package com.haeger.kafkachallenge.common.dto;

import java.math.BigDecimal;

public class ProductDto {
    private Long id;
    private Integer productCategoryid;
    private ProductCategoryDto category;
    private String sku;
    private String name;
    private BigDecimal price;
    private String specs;
}
