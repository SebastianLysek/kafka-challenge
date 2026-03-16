package com.haeger.kafkachallenge.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDto {
    private static final int CUSTOMER_EMAIL_MAX_LENGTH = 255;
    private static final int CUSTOMER_FULL_NAME_MAX_LENGTH = 255;
    private static final int CURRENCY_LENGTH = 3;
    private static final int NOTES_MAX_LENGTH = 65_535;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    @NotNull
    private Long customerId;

    @NotBlank
    @Email
    @Size(max = CUSTOMER_EMAIL_MAX_LENGTH)
    private String customerEmail;

    @NotBlank
    @Size(max = CUSTOMER_FULL_NAME_MAX_LENGTH)
    private String customerFullName;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private OrderStatus status;

    @NotBlank
    @Size(min = CURRENCY_LENGTH, max = CURRENCY_LENGTH)
    private String currency;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private BigDecimal total;

    @Size(max = NOTES_MAX_LENGTH)
    private String notes;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Instant createdAt;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Instant updatedAt;

    @Valid
    @NotEmpty
    @Builder.Default
    private Set<OrderItemDto> items = new LinkedHashSet<>();
}
