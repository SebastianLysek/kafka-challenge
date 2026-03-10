package com.haeger.kafkachallenge.common.events;

public class EventTypes {
    private EventTypes(){}

    public static final String PRODUCT_CATALOG_SNAPSHOT = "PRODUCT_CATALOG_SNAPSHOT";
    public static final String PRODUCT_UPSERTED = "PRODUCT_UPSERTED";

    public static final String ORDER_CREATED = "ORDER_CREATED";
    public static final String ORDER_CONFIRMED = "ORDER_CONFIRMED";
    public static final String ORDER_DECLINED = "ORDER_DECLINED";
    public static final String ORDER_SHIPPED = "ORDER_SHIPPED";

    public static final String ORDER_FULFILLMENT_CHECK_SUCCEEDED = "ORDER_FULFILLMENT_CHECK_SUCCEEDED";
    public static final String ORDER_FULFILLMENT_CHECK_FAILED = "ORDER_FULFILLMENT_CHECK_FAILED";

    public static final String SHIPMENT_PREPARATION_STARTED = "SHIPMENT_PREPARATION_STARTED";
}
