package com.haeger.kafkachallenge.customerrelations.model;

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
public class OrderEmailViewModel {
    private Long orderId;
    private String customerFullName;
    private String customerEmail;
    private String status;
    private String currency;
    private String total;
    private String createdAt;
    private String updatedAt;
    private String notes;
    private boolean hasNotes;

    @Builder.Default
    private List<OrderEmailItemViewModel> items = new ArrayList<>();
}
