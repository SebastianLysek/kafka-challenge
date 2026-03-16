package com.haeger.kafkachallenge.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.haeger.kafkachallenge.common.dto.InventoryItemDto;
import com.haeger.kafkachallenge.common.dto.OrderDto;
import com.haeger.kafkachallenge.common.dto.OrderStatus;
import com.haeger.kafkachallenge.common.dto.ShipmentDto;
import com.haeger.kafkachallenge.common.dto.ShipmentStatus;
import io.restassured.response.Response;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(OrderAnnotation.class)
class OrderWorkflowIT extends AbstractE2eIT {
    private static final String SUBJECT_PREFIX = "[Kafka Challenge] ";
    private static final String STARTUP_SMOKE_SKU = "CPU-AMD-7800X3D";
    private static final String HAPPY_PATH_SKU = "GPU-AMD-RX7800XT";
    private static final String DECLINED_SKU = "RAM-DDR4-16-3200";

    @BeforeAll
    static void waitForStack() {
        new OrderWorkflowIT().waitForFullStack();
    }

    @Test
    @Order(1)
    void createsOrderUsingSeededCatalogAfterStartupPropagation() {
        InventoryItemDto inventoryItem = findInventoryItemBySku(STARTUP_SMOKE_SKU);
        CustomerContact customer = newCustomer("startup-smoke");
        AtomicReference<OrderDto> createdOrder = new AtomicReference<>();

        Awaitility.await("seeded product becomes orderable")
            .atMost(AWAIT_TIMEOUT)
            .pollInterval(POLL_INTERVAL)
            .untilAsserted(() -> {
                Response response = tryCreateOrder(
                    customer,
                    inventoryItem.getProduct().getId(),
                    1,
                    "startup propagation smoke test"
                );

                if (response.statusCode() == 400) {
                    assertThat(response.asString()).contains("Unknown productId");
                    return;
                }

                assertThat(response.statusCode()).isEqualTo(201);
                createdOrder.set(parseOrder(response.asString()));
            });

        assertThat(createdOrder.get()).isNotNull();
        assertThat(createdOrder.get().getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(createdOrder.get().getItems()).singleElement()
            .satisfies(item -> assertThat(item.getProductId()).isEqualTo(inventoryItem.getProduct().getId()));
    }

    @Test
    @Order(2)
    void completesHappyPathAcrossInventoryOrderingShipmentAndMailhog() {
        InventoryItemDto inventoryItem = findInventoryItemBySku(HAPPY_PATH_SKU);
        CustomerContact customer = newCustomer("happy-path");
        String notes = "Leave this order with the warehouse gate team.";

        OrderDto createdOrder = createOrder(customer, inventoryItem.getProduct().getId(), 1, notes);

        assertThat(createdOrder.getStatus()).isEqualTo(OrderStatus.CREATED);

        OrderDto confirmedOrder = awaitOrderStatus(customer.customerId(), createdOrder.getId(), OrderStatus.CONFIRMED);
        OrderDto prepStartedOrder = awaitOrderStatus(customer.customerId(), createdOrder.getId(), OrderStatus.PREP_STARTED);

        ShipmentDto completedShipment = completeShipment(createdOrder.getId());
        assertThat(completedShipment.getOrderId()).isEqualTo(createdOrder.getId());
        assertThat(completedShipment.getStatus()).isEqualTo(ShipmentStatus.SHIPPED);

        OrderDto shippedOrder = awaitOrderStatus(customer.customerId(), createdOrder.getId(), OrderStatus.SHIPPED);
        List<MailhogMessage> messages = awaitMailMessages(customer.email(), createdOrder.getId(), 4);

        assertThat(confirmedOrder.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(prepStartedOrder.getStatus()).isEqualTo(OrderStatus.PREP_STARTED);
        assertThat(shippedOrder.getStatus()).isEqualTo(OrderStatus.SHIPPED);
        assertThat(messages).extracting(MailhogMessage::subject).containsExactlyInAnyOrder(
            SUBJECT_PREFIX + "We received your order #" + createdOrder.getId(),
            SUBJECT_PREFIX + "Your order #" + createdOrder.getId() + " is confirmed",
            SUBJECT_PREFIX + "Your order #" + createdOrder.getId() + " is being prepared",
            SUBJECT_PREFIX + "Your order #" + createdOrder.getId() + " has shipped"
        );
    }

    @Test
    @Order(3)
    void declinesOrderWhenRequestedQuantityExceedsInventory() {
        InventoryItemDto inventoryItem = findInventoryItemBySku(DECLINED_SKU);
        InventoryItemDto updatedInventory = updateInventoryQuantity(inventoryItem, 1);
        CustomerContact customer = newCustomer("declined");
        String notes = "This order should be declined because stock is too low.";

        OrderDto createdOrder = createOrder(customer, updatedInventory.getProduct().getId(), 2, notes);
        OrderDto declinedOrder = awaitOrderStatus(customer.customerId(), createdOrder.getId(), OrderStatus.DECLINED);
        List<MailhogMessage> messages = awaitMailMessages(customer.email(), createdOrder.getId(), 2);

        Response shipmentResponse = io.restassured.RestAssured.given()
            .baseUri(SHIPMENT_SERVICE_BASE_URL)
            .when()
            .post("/shipments/{orderId}/complete", createdOrder.getId());

        assertThat(createdOrder.getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(declinedOrder.getStatus()).isEqualTo(OrderStatus.DECLINED);
        assertThat(shipmentResponse.statusCode()).isEqualTo(404);
        assertThat(messages).extracting(MailhogMessage::subject).containsExactlyInAnyOrder(
            SUBJECT_PREFIX + "We received your order #" + createdOrder.getId(),
            SUBJECT_PREFIX + "We could not confirm order #" + createdOrder.getId()
        );
    }
}
