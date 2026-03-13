package com.haeger.kafkachallenge.customerrelations.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.haeger.kafkachallenge.common.dto.OrderStatus;
import com.haeger.kafkachallenge.common.events.EventTypes;
import com.haeger.kafkachallenge.common.events.OrderCreatedItemPayload;
import com.haeger.kafkachallenge.common.events.OrderCreatedPayload;
import com.haeger.kafkachallenge.customerrelations.config.MailProperties;
import com.haeger.kafkachallenge.customerrelations.util.mapper.PojoMapper;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

@ExtendWith(MockitoExtension.class)
class CustomerNotificationServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private CustomerNotificationService customerNotificationService;

    @BeforeEach
    void setUp() {
        customerNotificationService = new CustomerNotificationService(
            mailSender,
            templateEngine(),
            new MailProperties("no-reply@kafka-challenge.local", "[Kafka Challenge]"),
            new PojoMapper()
        );
    }

    @Test
    void sendsHtmlMailForOrderShippedEvent() throws Exception {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        customerNotificationService.sendOrderStatusUpdate(EventTypes.ORDER_SHIPPED, sampleOrder());

        verify(mailSender).send(mimeMessage);
        assertThat(mimeMessage.getAllRecipients()).singleElement().extracting(Object::toString).isEqualTo("max@example.com");
        assertThat(mimeMessage.getSubject()).isEqualTo("[Kafka Challenge] Your order #1001 has shipped");
        assertThat(mimeMessage.getContent().toString()).contains("Order shipped");
        assertThat(mimeMessage.getContent().toString()).contains("AMD Ryzen 7 7800X3D");
        assertThat(mimeMessage.getContent().toString()).contains("EUR 449.98");
        assertThat(mimeMessage.getContent().toString()).contains("Leave package at the side entrance.");
    }

    @Test
    void sendsDeclinedMailWithEventSpecificCopy() throws Exception {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        customerNotificationService.sendOrderStatusUpdate(EventTypes.ORDER_DECLINED, sampleOrder());

        verify(mailSender).send(mimeMessage);
        assertThat(mimeMessage.getSubject()).isEqualTo("[Kafka Challenge] We could not confirm order #1001");
        assertThat(mimeMessage.getContent().toString()).contains("Order declined");
        assertThat(mimeMessage.getContent().toString()).contains("current inventory situation");
    }

    private SpringTemplateEngine templateEngine() {
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateResolver.setCharacterEncoding("UTF-8");
        templateResolver.setCacheable(false);

        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);
        return templateEngine;
    }

    private OrderCreatedPayload sampleOrder() {
        return OrderCreatedPayload.builder()
            .orderId(1001L)
            .customerId(42L)
            .customerEmail("max@example.com")
            .customerFullName("Max Mustermann")
            .status(OrderStatus.SHIPPED)
            .currency("EUR")
            .total(new BigDecimal("449.98"))
            .notes("Leave package at the side entrance.")
            .createdAt(Instant.parse("2026-03-13T19:00:00Z"))
            .updatedAt(Instant.parse("2026-03-13T19:20:00Z"))
            .items(List.of(
                OrderCreatedItemPayload.builder()
                    .productId(10L)
                    .productSku("CPU-AMD-7800X3D")
                    .productName("AMD Ryzen 7 7800X3D")
                    .quantity(1)
                    .unitPrice(new BigDecimal("449.98"))
                    .lineTotal(new BigDecimal("449.98"))
                    .build()
            ))
            .build();
    }
}
