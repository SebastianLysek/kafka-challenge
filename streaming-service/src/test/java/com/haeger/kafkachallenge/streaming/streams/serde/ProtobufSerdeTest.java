package com.haeger.kafkachallenge.streaming.streams.serde;

import static org.assertj.core.api.Assertions.assertThat;

import com.haeger.kafkachallenge.streaming.proto.ProductUpserted;
import org.junit.jupiter.api.Test;

class ProtobufSerdeTest {
    @Test
    void roundTripsMessage() {
        ProductUpserted original = ProductUpserted.newBuilder()
            .setInventoryItemId(1)
            .setProductId(10)
            .setSku("CPU-AMD-7800X3D")
            .setName("AMD Ryzen 7 7800X3D")
            .setCurrency("EUR")
            .setPrice("399.99")
            .setQuantity(10)
            .build();

        ProtobufSerde<ProductUpserted> serde = new ProtobufSerde<>(ProductUpserted::parseFrom);

        byte[] bytes = serde.serializer().serialize("test", original);
        ProductUpserted restored = serde.deserializer().deserialize("test", bytes);

        assertThat(restored).isEqualTo(original);
    }
}
