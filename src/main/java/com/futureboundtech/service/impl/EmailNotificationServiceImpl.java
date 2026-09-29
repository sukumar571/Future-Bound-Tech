package com.futureboundtech.service.impl;

import com.futureboundtech.service.EmailNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Configuration-driven e-mail delivery.
 *
 * <p>Three independent switches have to line up before anything leaves the server:</p>
 * <ol>
 *   <li>{@code APP_EMAIL_ENABLED=true} — the feature flag;</li>
 *   <li>{@code APP_EMAIL_FROM} — the sender address, deliberately unset in code;</li>
 *   <li>{@code SPRING_MAIL_HOST} (plus {@code SPRING_MAIL_USERNAME} /
 *       {@code SPRING_MAIL_PASSWORD}) — which is what makes Spring Boot create a
 *       {@link JavaMailSender} bean in the first place.</li>
 * </ol>
 *
 * <p>Because {@link JavaMailSender} is injected through an {@link ObjectProvider},
 * the application still boots with no SMTP configuration at all — the notification
 * feature simply degrades to in-app only. Delivery runs on a background thread and
 * every failure is logged rather than propagated, so a mail outage can never roll
 * back an enrollment or a certificate.</p>
 */
@Slf4j
@Service
public class EmailNotificationServiceImpl implements EmailNotificationService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final Environment environment;

    @Value("${app.email.enabled:false}")
    private boolean enabled;

    /** No default on purpose: the address has not been decided yet. */
    @Value("${app.email.from:}")
    private String fromAddress;

    @Value("${app.email.subject-prefix:}")
    private String subjectPrefix;

    public EmailNotificationServiceImpl(ObjectProvider<JavaMailSender> mailSenderProvider,
                                        Environment environment) {
        this.mailSenderProvider = mailSenderProvider;
        this.environment = environment;
    }

    @Override
    public boolean isEnabled() {
        return statusReason() == null;
    }

    @Override
    public String statusReason() {
        if (!enabled) {
            return "E-mail is disabled. Set APP_EMAIL_ENABLED=true to turn it on.";
        }
        if (!StringUtils.hasText(fromAddress)) {
            return "Set APP_EMAIL_FROM to the address notifications should be sent from.";
        }
        if (!StringUtils.hasText(environment.getProperty("spring.mail.host"))) {
            return "Set SPRING_MAIL_HOST (and SPRING_MAIL_USERNAME / SPRING_MAIL_PASSWORD) to configure SMTP.";
        }
        if (mailSenderProvider.getIfAvailable() == null) {
            return "No SMTP sender is available for the configured spring.mail.* properties.";
        }
        return null;
    }

    @Override
    @Async("emailTaskExecutor")
    public void send(String recipientEmail, String subject, String body) {
        if (!isEnabled()) {
            return;
        }
        if (!StringUtils.hasText(recipientEmail)) {
            log.debug("Skipping e-mail with no recipient address: {}", subject);
            return;
        }
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(recipientEmail);
            message.setSubject(prefix() + subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Notification e-mail queued for {}", recipientEmail);
        } catch (RuntimeException ex) {
            // An inbox row is already persisted; a mail failure must never break the flow.
            log.warn("Could not send notification e-mail to {}: {}", recipientEmail, ex.getMessage());
        }
    }

    private String prefix() {
        return StringUtils.hasText(subjectPrefix) ? subjectPrefix.trim() + " " : "";
    }
}
