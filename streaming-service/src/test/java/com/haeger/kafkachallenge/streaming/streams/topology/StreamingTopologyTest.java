package com.haeger.kafkachallenge.streaming.streams.topology;

import static org.assertj.core.api.Assertions.assertThat;

import com.haeger.kafkachallenge.common.dto.OrderStatus;
import com.haeger.kafkachallenge.streaming.proto.OrderCreated;
import com.haeger.kafkachallenge.streaming.proto.OrderCreatedItem;
import com.haeger.kafkachallenge.streaming.proto.OrderStatusChanged;
import com.haeger.kafkachallenge.streaming.proto.ProductUpserted;
import com.haeger.kafkachallenge.streaming.streams.serde.ProtoSerdes;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.TestOutputTopic;
import org.apache.kafka.streams.TopologyTestDriver;
import org.junit.jupiter.api.Test;

class StreamingTopologyTest {
    @Test
    void confirmsOrderWithEnoughStock() {
        AtomicInteger reservations = new AtomicInteger();

        try (DriverFixture fixture = DriverFixture.open(order -> reservations.incrementAndGet())) {
            fixture.pipeProduct(10, 5);
            fixture.pipeOrder(1001, 10, 2);

            OrderStatusChanged result = fixture.readStatus();

            assertThat(result.getStatus()).isEqualTo(OrderStatus.CONFIRMED.name());
            assertThat(reservations).hasValue(1);
        }
    }

    @Test
    void declinesOrderWithTooLittleStock() {
        AtomicInteger reservations = new AtomicInteger();

        try (DriverFixture fixture = DriverFixture.open(order -> reservations.incrementAndGet())) {
            fixture.pipeProduct(10, 1);
            fixture.pipeOrder(1001, 10, 2);

            OrderStatusChanged result = fixture.readStatus();

            assertThat(result.getStatus()).isEqualTo(OrderStatus.DECLINED.name());
            assertThat(result.getReason()).contains("Insufficient inventory");
            assertThat(reservations).hasValue(0);
        }
    }

    @Test
    void declinesOrderWithUnknownProduct() {
        try (DriverFixture fixture = DriverFixture.open(order -> {
            throw new AssertionError("Reservation should not run for unknown products");
        })) {
            fixture.pipeOrder(1001, 999, 1);

            OrderStatusChanged result = fixture.readStatus();

            assertThat(result.getStatus()).isEqualTo(OrderStatus.DECLINED.name());
            assertThat(result.getReason()).contains("Unknown product");
        }
    }

    private static class DriverFixture implements AutoCloseable {
        private final TopologyTestDriver driver;
        private final TestInputTopic<String, ProductUpserted> productTopic;
        private final TestInputTopic<String, OrderCreated> orderTopic;
        private final TestOutputTopic<String, OrderStatusChanged> statusTopic;

        private DriverFixture(
            TopologyTestDriver driver,
            TestInputTopic<String, ProductUpserted> productTopic,
            TestInputTopic<String, OrderCreated> orderTopic,
            TestOutputTopic<String, OrderStatusChanged> statusTopic
        ) {
            this.driver = driver;
            this.productTopic = productTopic;
            this.orderTopic = orderTopic;
            this.statusTopic = statusTopic;
        }

        static DriverFixture open(java.util.function.Consumer<OrderCreated> reservation) {
            StreamsBuilder builder = new StreamsBuilder();
            StreamingTopology.buildTopology(builder, reservation);

            Properties properties = new Properties();
            properties.put(StreamsConfig.APPLICATION_ID_CONFIG, "streaming-topology-test");
            properties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:9092");
            properties.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.StringSerde.class);

            TopologyTestDriver driver = new TopologyTestDriver(builder.build(), properties);
            return new DriverFixture(
                driver,
                driver.createInputTopic(
                    StreamingTopics.PRODUCT_UPSERTED,
                    Serdes.String().serializer(),
                    ProtoSerdes.productUpserted().serializer()
                ),
                driver.createInputTopic(
                    StreamingTopics.ORDER_CREATED,
                    Serdes.String().serializer(),
                    ProtoSerdes.orderCreated().serializer()
                ),
                driver.createOutputTopic(
                    StreamingTopics.ORDER_STATUS_CHANGED,
                    Serdes.String().deserializer(),
                    ProtoSerdes.orderStatusChanged().deserializer()
                )
            );
        }

        void pipeProduct(long productId, int quantity) {
            productTopic.pipeInput(Long.toString(productId), ProductUpserted.newBuilder()
                .setInventoryItemId(productId)
                .setProductId(productId)
                .setSku("SKU-" + productId)
                .setName("Product " + productId)
                .setCurrency("EUR")
                .setPrice("10.00")
                .setQuantity(quantity)
                .build());
        }

        void pipeOrder(long orderId, long productId, int quantity) {
            orderTopic.pipeInput(Long.toString(orderId), OrderCreated.newBuilder()
                .setOrderId(orderId)
                .setCustomerId(42)
                .setCustomerEmail("customer@example.com")
                .setCustomerFullName("Customer")
                .setCurrency("EUR")
                .addItems(OrderCreatedItem.newBuilder()
                    .setProductId(productId)
                    .setQuantity(quantity)
                    .build())
                .build());
        }

        OrderStatusChanged readStatus() {
            return statusTopic.readValue();
        }

        @Override
        public void close() {
            driver.close();
        }
    }
}
