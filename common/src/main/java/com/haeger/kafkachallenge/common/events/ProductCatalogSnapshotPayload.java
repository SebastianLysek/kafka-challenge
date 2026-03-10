package com.haeger.kafkachallenge.common.events;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductCatalogSnapshotPayload {
    @Builder.Default
    private List<ProductCatalogProduct> products = new ArrayList<>();
}
