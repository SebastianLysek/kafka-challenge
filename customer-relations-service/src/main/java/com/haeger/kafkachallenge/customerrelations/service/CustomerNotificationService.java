package com.haeger.kafkachallenge.customerrelations.service;

import com.haeger.kafkachallenge.common.events.EventTypes;
import com.haeger.kafkachallenge.common.events.OrderCreatedPayload;
import com.haeger.kafkachallenge.customerrelations.config.MailProperties;
import com.haeger.kafkachallenge.customerrelations.util.mapper.PojoMapper;
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
    private final PojoMapper pojoMapper;

    /**
     * Sends an email notification to the customer with updates about their order status.
     * The email content is generated based on the provided event type and order information.
     *
     * @param eventType The type of event triggering the status update (e.g., order created, confirmed, shipped).
     * @param order The payload containing order details such as order ID, customer email, and items.
     * @throws IllegalArgumentException if the provided order payload is missing required fields.
     * @throws IllegalStateException if there is an issue preparing the email message.
     */
    public void sendOrderStatusUpdate(String eventType, OrderCreatedPayload order) {
        validateOrderSnapshot(order);
        TemplateSpec templateSpec = resolveTemplateSpec(eventType, order.getOrderId());

        Context context = new Context(Locale.ENGLISH);
        context.setVariable("mailTitle", templateSpec.title());
        context.setVariable("mailSummary", templateSpec.summary());
        context.setVariable("previewText", templateSpec.previewText());
        context.setVariable("statusLabel", templateSpec.statusLabel());
        context.setVariable("accentColor", templateSpec.accentColor());
        context.setVariable("order", pojoMapper.toOrderEmailViewModel(order));

        String html = templateEngine.process(TEMPLATE_NAME, context);
        MimeMessage mimeMessage = mailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, StandardCharsets.UTF_8.name());
            helper.setFrom(mailProperties.from());
            helper.setTo(order.getCustomerEmail());
            helper.setSubject(buildSubject(templateSpec.subject()));
            helper.setText(html, true);
        } catch (MessagingException ex) {
            throw new IllegalStateException("Failed to prepare order email", ex);
        }

        mailSender.send(mimeMessage);
        LOG.info("Sent {} mail for order {} to {}", eventType, order.getOrderId(), order.getCustomerEmail());
    }

    /**
     * Validates the provided order snapshot to ensure all required fields are present and not null or empty.
     * Throws an {@link IllegalArgumentException} if any validation criteria are not met.
     *
     * @param order the {@link OrderCreatedPayload} object representing the order snapshot to validate.
     *              Must not be null and must contain non-null, non-empty values for orderId, customerId,
     *              customerEmail, customerFullName, and items. Items list must not be empty.
     * @throws IllegalArgumentException if the order snapshot is null or if any of the required fields
     *                                  are null, blank, or empty.
     */
    private void validateOrderSnapshot(OrderCreatedPayload order) {
        if (order == null) {
            throw new IllegalArgumentException("Order snapshot must not be null");
        }
        if (order.getOrderId() == null) {
            throw new IllegalArgumentException("Order snapshot is missing orderId");
        }
        if (order.getCustomerId() == null) {
            throw new IllegalArgumentException("Order snapshot is missing customerId");
        }
        if (order.getCustomerEmail() == null || order.getCustomerEmail().isBlank()) {
            throw new IllegalArgumentException("Order snapshot is missing customerEmail");
        }
        if (order.getCustomerFullName() == null || order.getCustomerFullName().isBlank()) {
            throw new IllegalArgumentException("Order snapshot is missing customerFullName");
        }
        if (order.getItems() == null || order.getItems().isEmpty()) {
            throw new IllegalArgumentException("Order snapshot is missing items");
        }
    }

    /**
     * Resolves the appropriate {@link TemplateSpec} based on the given event type and order ID.
     *
     * This method generates a {@code TemplateSpec} object containing information
     * such as title, description, status, and other details that correspond to the
     * provided event type. If the event type does not match any supported values, an
     * {@link IllegalArgumentException} is thrown.
     *
     * @param eventType the type of the event (e.g., ORDER_CREATED, ORDER_CONFIRMED)
     *                  that determines the template details to be resolved. This must
     *                  match one of the predefined event types in {@link EventTypes}.
     * @param orderId the unique identifier of the order associated with the event.
     *                This is used to dynamically personalize the template content.
     * @return the {@link TemplateSpec} object containing the structured data
     *         for the given event type and order ID.
     * @throws IllegalArgumentException if the specified event type is unsupported or invalid.
     */
    private TemplateSpec resolveTemplateSpec(String eventType, Long orderId) {
        return switch (eventType) {
            case EventTypes.ORDER_CREATED -> new TemplateSpec(
                "We received your order #" + orderId,
                "Order received",
                "We received your order and are checking stock availability for every item.",
                "Created",
                "Your order has been registered successfully.",
                "#1d4ed8"
            );
            case EventTypes.ORDER_CONFIRMED -> new TemplateSpec(
                "Your order #" + orderId + " is confirmed",
                "Order confirmed",
                "Everything needed for your order is available and your order is now confirmed.",
                "Confirmed",
                "We are preparing the next warehouse steps for your order.",
                "#15803d"
            );
            case EventTypes.ORDER_DECLINED -> new TemplateSpec(
                "We could not confirm order #" + orderId,
                "Order declined",
                "We could not confirm the order with the current inventory situation.",
                "Declined",
                "Please review the order details below and place a new order if needed.",
                "#b91c1c"
            );
            case EventTypes.SHIPMENT_PREPARATION_STARTED -> new TemplateSpec(
                "Your order #" + orderId + " is being prepared",
                "Shipment preparation started",
                "Our warehouse has started packing your order for shipment.",
                "Preparation started",
                "The items below are currently being prepared for dispatch.",
                "#7c3aed"
            );
            case EventTypes.ORDER_SHIPPED -> new TemplateSpec(
                "Your order #" + orderId + " has shipped",
                "Order shipped",
                "Your order has left the warehouse and is on its way.",
                "Shipped",
                "Below is a summary of the shipped order.",
                "#0f766e"
            );
            default -> throw new IllegalArgumentException("Unsupported order event type %s".formatted(eventType));
        };
    }

    /**
     * Builds and returns the subject line for an email by optionally prepending
     * a predefined subject prefix.
     *
     * @param subject the original subject line to be modified or used as-is
     * @return the formatted subject line with the prefix prepended if it exists and is not blank,
     *         or the original subject if no valid prefix is available
     */
    private String buildSubject(String subject) {
        if (mailProperties.subjectPrefix() == null || mailProperties.subjectPrefix().isBlank()) {
            return subject;
        }
        return mailProperties.subjectPrefix() + " " + subject;
    }

    /**
     * A record representing the specification for a template.
     * This record encapsulates various attributes used to define the structure
     * and appearance of a template, such as its subject, title, and visual properties.
     *
     * @param subject       The subject of the template, representing its main purpose or topic.
     * @param title         The title of the template, indicating its primary heading or label.
     * @param summary       A brief summary or description of the template content.
     * @param statusLabel   A label representing the current status or state of the template.
     * @param previewText   Text used for previewing the template, usually a concise representative snippet.
     * @param accentColor   The accent color associated with the template, often used for branding or styling purposes.
     */
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
