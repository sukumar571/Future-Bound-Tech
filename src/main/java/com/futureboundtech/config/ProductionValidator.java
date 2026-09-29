package com.futureboundtech.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Production guard-rails, active only under the "prod" profile.
 *
 * <p>The base config intentionally ships placeholder values so local runs
 * never need secrets. In production those placeholders must never survive,
 * so this component refuses to let the application serve traffic with demo
 * payments, an insecure public URL, or a bootstrap admin still using the
 * default password.</p>
 */
@Slf4j
@Component
@org.springframework.context.annotation.Profile("prod")
public class ProductionValidator {

    /**
     * Exact values shipped as defaults in application.properties. Matching is
     * exact rather than "contains 'placeholder'" so that a genuine key which
     * merely embeds that word is never rejected — and never silently allowed.
     */
    private static final List<String> PLACEHOLDER_VALUES =
            List.of("rzp_test_placeholder", "placeholder_secret", "placeholder_webhook_secret");

    private final Environment env;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${razorpay.enabled}")
    private boolean razorpayEnabled;

    @Value("${razorpay.key.id:}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret:}")
    private String razorpayKeySecret;

    @Value("${razorpay.webhook.secret:}")
    private String razorpayWebhookSecret;

    @Value("${app.seeders.enabled:false}")
    private boolean seedersEnabled;

    @Value("${app.seed.admin-password:}")
    private String adminSeedPassword;

    public ProductionValidator(Environment env) {
        this.env = env;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void verifyProductionConfiguration() {
        List<String> problems = new ArrayList<>();

        // 1. Payments must be live and with real credentials — never the
        //    placeholders from application.properties, never blank.
        if (!razorpayEnabled) {
            problems.add("razorpay.enabled must be true in production (demo mode settles payments server-side).");
        }
        if (isPlaceholder(razorpayKeyId) || isPlaceholder(razorpayKeySecret) || isPlaceholder(razorpayWebhookSecret)) {
            problems.add("RAZORPAY_KEY_ID / RAZORPAY_KEY_SECRET / RAZORPAY_WEBHOOK_SECRET must be set to real "
                    + "gateway credentials (no placeholder values).");
        }
        if (razorpayKeyId != null && razorpayKeyId.startsWith("rzp_test_")) {
            problems.add("Razorpay TEST keys are not allowed in production — issue LIVE keys from the dashboard.");
        }

        // 2. The public URL must be HTTPS: payment callbacks, cookies and HSTS all depend on it.
        if (baseUrl == null || !baseUrl.startsWith("https://")) {
            problems.add("APP_BASE_URL must be an https:// URL in production (got: " + baseUrl + ").");
        }

        // 3. A bootstrap admin created with the demo password is an open door.
        if (seedersEnabled && (adminSeedPassword == null || adminSeedPassword.isBlank()
                || "password".equals(adminSeedPassword) || "changeme".equalsIgnoreCase(adminSeedPassword))) {
            problems.add("Seeders are enabled with a weak ADMIN_SEED_PASSWORD. Either finish bootstrapping and "
                    + "set BOOTSTRAP_SEEDS=false, or supply a strong one-time ADMIN_SEED_PASSWORD.");
        }

        // 4. H2 must not be reachable through the prod datasource by accident.
        String jdbcUrl = env.getProperty("spring.datasource.url", "");
        if (jdbcUrl.startsWith("jdbc:h2:")) {
            problems.add("Production datasource points at H2 (" + jdbcUrl + "). Set DB_* variables for MySQL.");
        }

        if (!problems.isEmpty()) {
            String msg = "Refusing to serve production traffic. Fix the following, then restart:\n  - "
                    + String.join("\n  - ", problems);
            log.error(msg);
            throw new IllegalStateException(msg);
        }
        log.info("Production configuration checks passed (base URL {}, live-mode payments).", baseUrl);
    }

    private boolean isPlaceholder(String value) {
        if (value == null || value.isBlank()) {
            return true;
        }
        return PLACEHOLDER_VALUES.contains(value.trim().toLowerCase(java.util.Locale.ROOT));
    }
}
