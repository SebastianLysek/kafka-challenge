package com.haeger.kafkachallenge.streaming.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.mail")
public record MailProperties(
    String from,
    String subjectPrefix
) {
}
