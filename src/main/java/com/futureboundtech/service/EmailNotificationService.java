package com.futureboundtech.service;

/**
 * Optional e-mail delivery for notifications.
 *
 * <p>The institute has not decided its notification address yet, so this service
 * ships switched&nbsp;off: nothing is sent unless it is explicitly enabled and a
 * sender address is supplied through configuration. SMTP credentials come from
 * environment variables only — no address, username or password lives in the code
 * or in {@code application.properties}.</p>
 */
public interface EmailNotificationService {

    /** True when e-mail is enabled, a sender address is configured and SMTP is reachable. */
    boolean isEnabled();

    /** Human-readable reason e-mail is off, for the admin status panel. */
    String statusReason();

    /** Sends one mail. Never throws — a mail outage must not fail the product flow. */
    void send(String recipientEmail, String subject, String body);
}
