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
public class OrderFulfillmentCheckResultPayload {
    private Long orderId;
    private String message;

    @Builder.Default
    private List<OrderFulfillmentCheckItem> items = new ArrayList<>();
}
