package com.futureboundtech.config;

import com.razorpay.RazorpayClient;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Razorpay configuration. Credentials are supplied through environment
 * variables (RAZORPAY_KEY_ID, RAZORPAY_KEY_SECRET, RAZORPAY_WEBHOOK_SECRET)
 * and must never be committed to source control.
 */
@Configuration
@Getter
public class RazorpayConfig {

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    @Value("${razorpay.webhook.secret}")
    private String webhookSecret;

    @Value("${razorpay.enabled:false}")
    private boolean enabled;

    /**
     * Constructing the client performs no network I/O, so it is safe to create
     * even while running with placeholder test credentials.
     */
    @Bean
    public RazorpayClient razorpayClient() throws Exception {
        return new RazorpayClient(keyId, keySecret);
    }
}
