package com.haeger.kafkachallenge.e2e;

import static io.restassured.config.ObjectMapperConfig.objectMapperConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.haeger.kafkachallenge.common.dto.InventoryItemDto;
import com.haeger.kafkachallenge.common.dto.OrderDto;
import com.haeger.kafkachallenge.common.dto.OrderItemDto;
import com.haeger.kafkachallenge.common.dto.OrderStatus;
import com.haeger.kafkachallenge.common.dto.ProductDto;
import com.haeger.kafkachallenge.common.dto.ShipmentDto;
import com.haeger.kafkachallenge.common.dto.ShipmentStatus;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeAll;

abstract class AbstractE2eIT {
    private static final AtomicLong CUSTOMER_SEQUENCE = new AtomicLong(System.currentTimeMillis());
    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
        .findAndAddModules()
        .build();

    protected static final String ORDER_SERVICE_BASE_URL = System.getProperty(
        "e2e.order-service.base-url",
        "http://localhost:8081"
    );
    protected static final String INVENTORY_SERVICE_BASE_URL = System.getProperty(
        "e2e.inventory-service.base-url",
        "http://localhost:8082"
    );
    protected static final String SHIPMENT_SERVICE_BASE_URL = System.getProperty(
        "e2e.shipment-service.base-url",
        "http://localhost:8083"
    );
    protected static final String CUSTOMER_RELATIONS_SERVICE_BASE_URL = System.getProperty(
        "e2e.customer-relations-service.base-url",
        "http://localhost:8084"
    );
    protected static final String MAILHOG_BASE_URL = System.getProperty(
        "e2e.mailhog.base-url",
        "http://localhost:8025"
    );
    protected static final Duration AWAIT_TIMEOUT = Duration.ofSeconds(
        Long.parseLong(System.getProperty("e2e.await.timeout.seconds", "120"))
    );
    protected static final Duration POLL_INTERVAL = Duration.ofMillis(
        Long.parseLong(System.getProperty("e2e.await.poll.interval.millis", "500"))
    );

    @BeforeAll
    static void configureRestAssured() {
        RestAssured.config = RestAssured.config().objectMapperConfig(
            objectMapperConfig().jackson2ObjectMapperFactory((type, charset) -> OBJECT_MAPPER)
        );
    }

    protected void waitForFullStack() {
        waitForServiceHealth("order-service", ORDER_SERVICE_BASE_URL);
        waitForServiceHealth("inventory-service", INVENTORY_SERVICE_BASE_URL);
        waitForServiceHealth("shipment-service", SHIPMENT_SERVICE_BASE_URL);
        waitForServiceHealth("customer-relations-service", CUSTOMER_RELATIONS_SERVICE_BASE_URL);

        Awaitility.await("mailhog")
            .ignoreExceptions()
            .atMost(AWAIT_TIMEOUT)
            .pollInterval(POLL_INTERVAL)
            .untilAsserted(() -> {
                Response response = RestAssured.given()
                    .baseUri(MAILHOG_BASE_URL)
                    .when()
                    .get("/api/v2/messages");

                assertThat(response.statusCode()).isEqualTo(200);
            });
    }

    protected InventoryItemDto findInventoryItemBySku(String sku) {
        AtomicReference<InventoryItemDto> observedItem = new AtomicReference<>();

        Awaitility.await("inventory item " + sku)
            .ignoreExceptions()
            .atMost(AWAIT_TIMEOUT)
            .pollInterval(POLL_INTERVAL)
            .untilAsserted(() -> {
                InventoryItemDto item = findInventoryItemFromPayload(fetchInventoryPayload(), sku);

                assertThat(item).isNotNull();
                observedItem.set(item);
            });

        return observedItem.get();
    }

    protected OrderDto createOrder(CustomerContact customer, Long productId, int quantity, String notes) {
        return parseOrder(
            RestAssured.given()
            .baseUri(ORDER_SERVICE_BASE_URL)
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(orderRequestBody(customer, productId, quantity, notes))
            .when()
            .post("/orders")
            .then()
            .statusCode(201)
            .extract()
            .asString()
        );
    }

    protected Response tryCreateOrder(CustomerContact customer, Long productId, int quantity, String notes) {
        return RestAssured.given()
            .baseUri(ORDER_SERVICE_BASE_URL)
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(orderRequestBody(customer, productId, quantity, notes))
            .when()
            .post("/orders");
    }

    protected OrderDto awaitOrderStatus(Long customerId, Long orderId, OrderStatus expectedStatus) {
        AtomicReference<OrderDto> observedOrder = new AtomicReference<>();

        Awaitility.await("order " + orderId + " -> " + expectedStatus)
            .atMost(AWAIT_TIMEOUT)
            .pollInterval(POLL_INTERVAL)
            .untilAsserted(() -> {
                OrderDto order = findCustomerOrderOrNull(customerId, orderId);
                assertThat(order).isNotNull();
                assertThat(order.getStatus()).isEqualTo(expectedStatus);
                observedOrder.set(order);
            });

        return observedOrder.get();
    }

    protected OrderDto findCustomerOrder(Long customerId, Long orderId) {
        OrderDto order = findCustomerOrderOrNull(customerId, orderId);
        if (order == null) {
            throw new IllegalStateException("Customer %d does not have order %d yet".formatted(customerId, orderId));
        }
        return order;
    }

    protected List<OrderDto> listOrdersByCustomer(Long customerId) {
        String body = RestAssured.given()
            .baseUri(ORDER_SERVICE_BASE_URL)
            .accept(ContentType.JSON)
            .when()
            .get("/customers/{customerId}/orders", customerId)
            .then()
            .statusCode(200)
            .extract()
            .asString();

        try {
            JsonNode payload = OBJECT_MAPPER.readTree(body);
            if (!payload.isArray()) {
                throw new IllegalStateException("Expected customer orders payload to be an array but got: " + payload);
            }

            List<OrderDto> orders = new ArrayList<>();
            for (JsonNode orderNode : payload) {
                orders.add(parseOrder(orderNode));
            }
            return orders;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse customer orders payload: " + body, ex);
        }
    }

    protected InventoryItemDto updateInventoryQuantity(InventoryItemDto item, int quantity) {
        String body = RestAssured.given()
            .baseUri(INVENTORY_SERVICE_BASE_URL)
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(Map.of(
                "productId", item.getProduct().getId(),
                "quantity", quantity
            ))
            .when()
            .put("/inventory/{id}", item.getId())
            .then()
            .statusCode(200)
            .extract()
            .asString();

        try {
            return findInventoryItemFromPayload(OBJECT_MAPPER.readTree("[" + body + "]"), item.getProduct().getSku());
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse inventory update response: " + body, ex);
        }
    }

    protected ShipmentDto completeShipment(Long orderId) {
        return parseShipment(
            RestAssured.given()
            .baseUri(SHIPMENT_SERVICE_BASE_URL)
            .accept(ContentType.JSON)
            .when()
            .post("/shipments/{orderId}/complete", orderId)
            .then()
            .statusCode(200)
            .extract()
            .asString()
        );
    }

    protected List<MailhogMessage> awaitMailMessages(String email, Long orderId, int expectedCount) {
        return Awaitility.await("mailhog messages for " + email)
            .atMost(AWAIT_TIMEOUT)
            .pollInterval(POLL_INTERVAL)
            .until(() -> getMailMessages(email, orderId), messages -> messages.size() == expectedCount);
    }

    protected List<MailhogMessage> getMailMessages(String email, Long orderId) {
        JsonNode response = RestAssured.given()
            .baseUri(MAILHOG_BASE_URL)
            .accept(ContentType.JSON)
            .when()
            .get("/api/v2/messages")
            .then()
            .statusCode(200)
            .extract()
            .as(JsonNode.class);

        List<MailhogMessage> matches = new ArrayList<>();
        for (JsonNode item : response.path("items")) {
            String toHeader = readFirstHeader(item, "To");
            String subject = readFirstHeader(item, "Subject");
            String body = item.path("Content").path("Body").asText("");
            if (toHeader.contains(email) && (subject.contains("#" + orderId) || body.contains("#" + orderId))) {
                matches.add(new MailhogMessage(subject, body));
            }
        }

        matches.sort(Comparator.comparing(MailhogMessage::subject));
        return matches;
    }

    protected CustomerContact newCustomer(String scenarioName) {
        long customerId = CUSTOMER_SEQUENCE.incrementAndGet();
        String email = "e2e+" + scenarioName + "-" + customerId + "@kafka-challenge.local";
        return new CustomerContact(customerId, email, "E2E " + scenarioName + " " + customerId);
    }

    protected void waitForServiceHealth(String serviceName, String baseUrl) {
        Awaitility.await(serviceName + " health")
            .ignoreExceptions()
            .atMost(AWAIT_TIMEOUT)
            .pollInterval(POLL_INTERVAL)
            .untilAsserted(() -> {
                Response response = RestAssured.given()
                    .baseUri(baseUrl)
                    .accept(ContentType.JSON)
                    .when()
                    .get("/actuator/health");

                assertThat(response.statusCode()).isEqualTo(200);
                assertThat(response.jsonPath().getString("status")).isEqualTo("UP");
            });
    }

    private String readFirstHeader(JsonNode item, String headerName) {
        JsonNode values = item.path("Content").path("Headers").path(headerName);
        if (!values.isArray() || values.isEmpty()) {
            return "";
        }
        return values.get(0).asText("");
    }

    private OrderDto findCustomerOrderOrNull(Long customerId, Long orderId) {
        return listOrdersByCustomer(customerId).stream()
            .filter(order -> orderId.equals(order.getId()))
            .findFirst()
            .orElse(null);
    }

    private JsonNode fetchInventoryPayload() {
        String body = RestAssured.given()
            .baseUri(INVENTORY_SERVICE_BASE_URL)
            .accept(ContentType.JSON)
            .when()
            .get("/inventory")
            .then()
            .statusCode(200)
            .extract()
            .asString();

        try {
            return OBJECT_MAPPER.readTree(body);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse inventory payload: " + body, ex);
        }
    }

    private InventoryItemDto findInventoryItemFromPayload(JsonNode payload, String sku) {
        if (!payload.isArray()) {
            throw new IllegalStateException("Expected inventory payload to be a JSON array but got: " + payload);
        }

        for (JsonNode candidate : payload) {
            JsonNode product = candidate.path("product");
            if (!sku.equals(product.path("sku").asText())) {
                continue;
            }

            return InventoryItemDto.builder()
                .id(candidate.path("id").asLong())
                .productId(product.path("id").asLong())
                .product(ProductDto.builder()
                    .id(readLong(product, "id"))
                    .sku(readText(product, "sku"))
                    .name(readText(product, "name"))
                    .price(readDecimal(product, "price"))
                    .specs(readNullableText(product, "specs"))
                    .build())
                .quantity(candidate.path("quantity").asInt())
                .build();
        }

        return null;
    }

    private Map<String, Object> orderRequestBody(CustomerContact customer, Long productId, int quantity, String notes) {
        return Map.of(
            "customerId", customer.customerId(),
            "customerEmail", customer.email(),
            "customerFullName", customer.fullName(),
            "currency", "EUR",
            "notes", notes,
            "items", List.of(Map.of(
                "productId", productId,
                "quantity", quantity
            ))
        );
    }

    protected OrderDto parseOrder(String body) {
        try {
            return parseOrder(OBJECT_MAPPER.readTree(body));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse order payload: " + body, ex);
        }
    }

    private OrderDto parseOrder(JsonNode orderNode) {
        List<OrderItemDto> items = new ArrayList<>();
        JsonNode itemNodes = orderNode.path("items");
        if (itemNodes.isArray()) {
            for (JsonNode itemNode : itemNodes) {
                items.add(OrderItemDto.builder()
                    .id(readLong(itemNode, "id"))
                    .orderId(readLong(itemNode, "orderId"))
                    .productId(readLong(itemNode, "productId"))
                    .productSku(readNullableText(itemNode, "productSku"))
                    .productName(readNullableText(itemNode, "productName"))
                    .unitPrice(readDecimal(itemNode, "unitPrice"))
                    .quantity(itemNode.path("quantity").isMissingNode() ? null : itemNode.path("quantity").asInt())
                    .lineTotal(readDecimal(itemNode, "lineTotal"))
                    .build());
            }
        }

        return OrderDto.builder()
            .id(readLong(orderNode, "id"))
            .customerId(readLong(orderNode, "customerId"))
            .customerEmail(readText(orderNode, "customerEmail"))
            .customerFullName(readText(orderNode, "customerFullName"))
            .status(readEnum(orderNode, "status", OrderStatus.class))
            .currency(readText(orderNode, "currency"))
            .total(readDecimal(orderNode, "total"))
            .notes(readNullableText(orderNode, "notes"))
            .createdAt(readInstant(orderNode, "createdAt"))
            .updatedAt(readInstant(orderNode, "updatedAt"))
            .items(new LinkedHashSet<>(items))
            .build();
    }

    private ShipmentDto parseShipment(String body) {
        try {
            JsonNode shipmentNode = OBJECT_MAPPER.readTree(body);
            return ShipmentDto.builder()
                .id(readLong(shipmentNode, "id"))
                .orderId(readLong(shipmentNode, "orderId"))
                .customerId(readLong(shipmentNode, "customerId"))
                .customerEmail(readText(shipmentNode, "customerEmail"))
                .customerFullName(readText(shipmentNode, "customerFullName"))
                .status(readEnum(shipmentNode, "status", ShipmentStatus.class))
                .createdAt(readInstant(shipmentNode, "createdAt"))
                .updatedAt(readInstant(shipmentNode, "updatedAt"))
                .preparationStartedAt(readInstant(shipmentNode, "preparationStartedAt"))
                .shippedAt(readInstant(shipmentNode, "shippedAt"))
                .build();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse shipment payload: " + body, ex);
        }
    }

    private Long readLong(JsonNode node, String fieldName) {
        JsonNode field = node.path(fieldName);
        return field.isMissingNode() || field.isNull() ? null : field.asLong();
    }

    private BigDecimal readDecimal(JsonNode node, String fieldName) {
        JsonNode field = node.path(fieldName);
        return field.isMissingNode() || field.isNull() ? null : field.decimalValue();
    }

    private Instant readInstant(JsonNode node, String fieldName) {
        JsonNode field = node.path(fieldName);
        if (field.isMissingNode() || field.isNull() || field.asText().isBlank()) {
            return null;
        }
        return Instant.parse(field.asText());
    }

    private String readText(JsonNode node, String fieldName) {
        JsonNode field = node.path(fieldName);
        return field.isMissingNode() || field.isNull() ? null : field.asText();
    }

    private String readNullableText(JsonNode node, String fieldName) {
        return readText(node, fieldName);
    }

    private <T extends Enum<T>> T readEnum(JsonNode node, String fieldName, Class<T> enumType) {
        String value = readText(node, fieldName);
        return value == null || value.isBlank() ? null : Enum.valueOf(enumType, value);
    }

    protected record CustomerContact(Long customerId, String email, String fullName) {
    }

    protected record MailhogMessage(String subject, String body) {
    }
}
