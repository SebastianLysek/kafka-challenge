package com.haeger.kafkachallenge.streaming.notification.service;

import com.haeger.kafkachallenge.common.dto.OrderStatus;
import com.haeger.kafkachallenge.streaming.config.DtoMapper;
import com.haeger.kafkachallenge.streaming.notification.config.MailProperties;
import com.haeger.kafkachallenge.streaming.order.entity.Order;
import com.haeger.kafkachallenge.streaming.order.repository.OrderRepository;
import com.haeger.kafkachallenge.streaming.proto.NotificationRequested;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
@RequiredArgsConstructor
public class CustomerNotificationService {
    private static final Logger LOG = LoggerFactory.getLogger(CustomerNotificationService.class);
    private static final String TEMPLATE_NAME = "mail/order-status-update";

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    private final MailProperties mailProperties;
    private final OrderRepository orderRepository;
    private final DtoMapper dtoMapper;

    public void sendOrderStatusUpdate(NotificationRequested event) {
        Order order = orderRepository.findById(event.getOrderId())
            .orElseThrow(() -> new IllegalStateException("Order %d not found".formatted(event.getOrderId())));
        OrderStatus status = OrderStatus.valueOf(event.getOrderStatus());
        TemplateSpec templateSpec = resolveTemplateSpec(status, event.getOrderId());

        Context context = new Context(Locale.ENGLISH);
        context.setVariable("mailTitle", templateSpec.title());
        context.setVariable("mailSummary", templateSpec.summary());
        context.setVariable("previewText", templateSpec.previewText());
        context.setVariable("statusLabel", templateSpec.statusLabel());
        context.setVariable("accentColor", templateSpec.accentColor());
        context.setVariable("order", dtoMapper.toOrderDto(order));

        String html = templateEngine.process(TEMPLATE_NAME, context);
        MimeMessage mimeMessage = mailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, StandardCharsets.UTF_8.name());
            helper.setFrom(mailProperties.from());
            helper.setTo(event.getCustomerEmail());
            helper.setSubject(buildSubject(templateSpec.subject()));
            helper.setText(html, true);
        } catch (MessagingException ex) {
            throw new IllegalStateException("Failed to prepare order email", ex);
        }

        mailSender.send(mimeMessage);
        LOG.info("Sent {} mail for order {} to {}", status, event.getOrderId(), event.getCustomerEmail());
    }

    private TemplateSpec resolveTemplateSpec(OrderStatus status, long orderId) {
        return switch (status) {
            case CREATED -> new TemplateSpec(
                "We received your order #" + orderId,
                "Order received",
                "We received your order and are checking stock availability for every item.",
                "Created",
                "Your order has been registered successfully.",
                "#1d4ed8"
            );
            case CONFIRMED -> new TemplateSpec(
                "Your order #" + orderId + " is confirmed",
                "Order confirmed",
                "Everything needed for your order is available and your order is now confirmed.",
                "Confirmed",
                "We are preparing the next warehouse steps for your order.",
                "#15803d"
            );
            case DECLINED -> new TemplateSpec(
                "We could not confirm order #" + orderId,
                "Order declined",
                "We could not confirm the order with the current inventory situation.",
                "Declined",
                "Please review the order details below and place a new order if needed.",
                "#b91c1c"
            );
            case PREP_STARTED -> new TemplateSpec(
                "Your order #" + orderId + " is being prepared",
                "Shipment preparation started",
                "Our warehouse has started packing your order for shipment.",
                "Preparation started",
                "The items below are currently being prepared for dispatch.",
                "#7c3aed"
            );
            case SHIPPED -> new TemplateSpec(
                "Your order #" + orderId + " has shipped",
                "Order shipped",
                "Your order has left the warehouse and is on its way.",
                "Shipped",
                "Below is a summary of the shipped order.",
                "#0f766e"
            );
        };
    }

    private String buildSubject(String subject) {
        if (mailProperties.subjectPrefix() == null || mailProperties.subjectPrefix().isBlank()) {
            return subject;
        }
        return mailProperties.subjectPrefix() + " " + subject;
    }

    private record TemplateSpec(
        String subject,
        String title,
        String summary,
        String statusLabel,
        String previewText,
        String accentColor
    ) {
    }
}
