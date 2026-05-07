package com.haeger.kafkachallenge.streaming.streams.topology;

public final class StreamingTopics {
    private StreamingTopics() {
    }

    public static final String PRODUCT_UPSERTED = "streaming.product-upserted-events";
    public static final String ORDER_CREATED = "streaming.order-created-events";
    public static final String ORDER_STATUS_CHANGED = "streaming.order-status-changed-events";
    public static final String SHIPMENT_PREPARATION_STARTED = "streaming.shipment-preparation-started-events";
    public static final String SHIPMENT_COMPLETED = "streaming.shipment-completed-events";
    public static final String NOTIFICATION_REQUESTED = "streaming.notification-requested-events";

    public static final String INVENTORY_STORE = "inventory-store";
}
