package com.haeger.kafkachallenge.streaming.notification.service;

import com.haeger.kafkachallenge.streaming.proto.NotificationRequested;
import com.haeger.kafkachallenge.streaming.streams.topology.StreamingTopics;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationEventListener {
    private final CustomerNotificationService customerNotificationService;

    @KafkaListener(topics = StreamingTopics.NOTIFICATION_REQUESTED, groupId = "streaming-service-notifications")
    public void onNotificationRequested(byte[] payload) throws Exception {
        customerNotificationService.sendOrderStatusUpdate(NotificationRequested.parseFrom(payload));
    }
}
