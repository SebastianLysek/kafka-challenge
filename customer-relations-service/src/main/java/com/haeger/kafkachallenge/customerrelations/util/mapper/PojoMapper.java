package com.haeger.kafkachallenge.customerrelations.util.mapper;

import com.haeger.kafkachallenge.common.dto.OrderStatus;
import com.haeger.kafkachallenge.common.events.OrderCreatedItemPayload;
import com.haeger.kafkachallenge.common.events.OrderCreatedPayload;
import com.haeger.kafkachallenge.customerrelations.model.OrderEmailItemViewModel;
import com.haeger.kafkachallenge.customerrelations.model.OrderEmailViewModel;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PojoMapper {
    private static final String MISSING_VALUE = "-";
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");

    /**
     * Converts an {@link OrderCreatedPayload} object into an {@link OrderEmailViewModel} object.
     *
     * @param source the {@link OrderCreatedPayload} containing order details to be mapped.
     *               If the source is null, the method returns null.
     * @return an {@link OrderEmailViewModel} built from the provided {@link OrderCreatedPayload},
     *         or null if the input source is null.
     */
    public OrderEmailViewModel toOrderEmailViewModel(OrderCreatedPayload source) {
        if (source == null) {
            return null;
        }

        List<OrderEmailItemViewModel> items = source.getItems() == null
            ? List.of()
            : source.getItems().stream()
                .map(item -> toOrderEmailItemViewModel(item, source.getCurrency()))
                .toList();

        String notes = source.getNotes() != null ? source.getNotes().trim() : "";
        return OrderEmailViewModel.builder()
            .orderId(source.getOrderId())
            .customerFullName(source.getCustomerFullName())
            .customerEmail(source.getCustomerEmail())
            .status(formatOrderStatus(source.getStatus()))
            .currency(defaultValue(source.getCurrency()))
            .total(formatAmount(source.getTotal(), source.getCurrency()))
            .createdAt(formatInstant(source.getCreatedAt()))
            .updatedAt(formatInstant(source.getUpdatedAt()))
            .notes(notes)
            .hasNotes(!notes.isBlank())
            .items(items)
            .build();
    }

    /**
     * Maps an {@link OrderCreatedItemPayload} object to an {@link OrderEmailItemViewModel}.
     * Converts the fields of the source object into a corresponding view model, formatting
     * specific values as necessary. Returns null if the input source is null.
     *
     * @param source the {@link OrderCreatedItemPayload} containing item details to be mapped.
     *               If source is null, the method will return null.
     * @param currency the currency used for formatting monetary values. If null or blank,
     *                 no currency code will be included in the formatted amounts.
     * @return an {@link OrderEmailItemViewModel} built from the provided source, with formatted
     *         and defaulted values, or null if the input source is null.
     */
    private OrderEmailItemViewModel toOrderEmailItemViewModel(OrderCreatedItemPayload source, String currency) {
        if (source == null) {
            return null;
        }

        return OrderEmailItemViewModel.builder()
            .productSku(defaultValue(source.getProductSku()))
            .productName(defaultValue(source.getProductName()))
            .quantity(source.getQuantity())
            .unitPrice(formatAmount(source.getUnitPrice(), currency))
            .lineTotal(formatAmount(source.getLineTotal(), currency))
            .build();
    }

    /**
     * Formats the given {@link OrderStatus} into a human-readable string representation.
     * If the input status is null, a default placeholder value is returned.
     *
     * @param status the {@link OrderStatus} to be formatted. If null, a default placeholder is returned.
     * @return a string representation of the order status, or a default placeholder if the input is null.
     */
    private String formatOrderStatus(OrderStatus status) {
        if (status == null) {
            return MISSING_VALUE;
        }

        return switch (status) {
            case CREATED -> "Created";
            case CONFIRMED -> "Confirmed";
            case DECLINED -> "Declined";
            case PREP_STARTED -> "Preparation started";
            case SHIPPED -> "Shipped";
        };
    }

    /**
     * Formats a monetary amount with optional currency information.
     * If the input amount is null, a placeholder value is returned.
     * If the currency is null or blank, no currency code is included in the result.
     *
     * @param amount the monetary amount to format. If null, a placeholder value is returned.
     * @param currency the currency code to prepend to the formatted amount.
     *                 If null or blank, no currency code is included.
     * @return a string representing the formatted amount, optionally prefixed with the currency code,
     *         or a placeholder value if the input amount is null.
     */
    private String formatAmount(BigDecimal amount, String currency) {
        if (amount == null) {
            return MISSING_VALUE;
        }

        String currencyCode = currency == null || currency.isBlank() ? "" : currency.trim() + " ";
        return currencyCode + amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    /**
     * Formats the given {@link Instant} into a string representation
     * based on the system's default time zone and a predefined timestamp format.
     * If the provided {@link Instant} is null, a placeholder value is returned.
     *
     * @param instant the {@link Instant} to be formatted. If null, a placeholder value is returned.
     * @return a string representation of the {@link Instant}, or a placeholder value if the input is null.
     */
    private String formatInstant(Instant instant) {
        if (instant == null) {
            return MISSING_VALUE;
        }

        return TIMESTAMP_FORMATTER.format(instant.atZone(ZoneId.systemDefault()));
    }

    /**
     * Returns a default value if the input string is null or blank.
     *
     * @param value the input string to evaluate. If the string is null or blank,
     *              a predefined default value is returned.
     * @return the input string if it is non-null and non-blank; otherwise,
     *         a predefined default value.
     */
    private String defaultValue(String value) {
        if (value == null || value.isBlank()) {
            return MISSING_VALUE;
        }
        return value;
    }
}
