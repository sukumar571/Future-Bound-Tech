package com.futureboundtech.unit;

import com.futureboundtech.config.ProductionValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The prod-profile guard-rail: it must accept a correct live configuration and
 * refuse (with actionable messages) every footgun the base config allows —
 * placeholder keys, test keys in production, plain-HTTP base URLs, demo
 * seeding with a weak admin password, or an accidental H2 datasource.
 */
class ProductionValidatorTest {

    private ProductionValidator validator(String baseUrl, boolean razorpayEnabled,
                                          String keyId, String keySecret, String webhookSecret,
                                          boolean seeders, String adminSeedPassword,
                                          String jdbcUrl) {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("spring.datasource.url", jdbcUrl);
        ProductionValidator v = new ProductionValidator(env);
        ReflectionTestUtils.setField(v, "baseUrl", baseUrl);
        ReflectionTestUtils.setField(v, "razorpayEnabled", razorpayEnabled);
        ReflectionTestUtils.setField(v, "razorpayKeyId", keyId);
        ReflectionTestUtils.setField(v, "razorpayKeySecret", keySecret);
        ReflectionTestUtils.setField(v, "razorpayWebhookSecret", webhookSecret);
        ReflectionTestUtils.setField(v, "seedersEnabled", seeders);
        ReflectionTestUtils.setField(v, "adminSeedPassword", adminSeedPassword);
        return v;
    }

    private ProductionValidator healthy() {
        return validator("https://futurebound.example", true,
                "rzp_live_REALID123", "real_secret_value", "real_webhook_secret",
                false, "", "jdbc:mysql://localhost:3306/future_bound_tech_db");
    }

    @Test
    @DisplayName("a correct live configuration passes without exception")
    void healthyConfigAccepted() {
        assertDoesNotThrow(() -> healthy().verifyProductionConfiguration());
    }

    @Test
    @DisplayName("placeholder / missing Razorpay credentials are rejected")
    void placeholderKeysRejected() {
        ProductionValidator v = validator("https://futurebound.example", true,
                "rzp_test_placeholder", "placeholder_secret", "",
                false, "", "jdbc:mysql://localhost:3306/fbt");
        IllegalStateException ex =
                assertThrows(IllegalStateException.class, v::verifyProductionConfiguration);
        assertTrue(ex.getMessage().contains("RAZORPAY_KEY_ID"));
    }

    @Test
    @DisplayName("test-mode keys are never allowed under the prod profile")
    void testKeysRejected() {
        ProductionValidator v = validator("https://futurebound.example", true,
                "rzp_test_ABC123", "real_secret", "real_webhook",
                false, "", "jdbc:mysql://localhost:3306/fbt");
        IllegalStateException ex =
                assertThrows(IllegalStateException.class, v::verifyProductionConfiguration);
        assertTrue(ex.getMessage().contains("TEST keys"));
    }

    @Test
    @DisplayName("disabled gateway (demo settlement mode) is rejected in production")
    void demoModeRejected() {
        ProductionValidator v = validator("https://futurebound.example", false,
                "rzp_live_ABC", "secret", "webhook", false, "", "jdbc:mysql://localhost:3306/fbt");
        IllegalStateException ex =
                assertThrows(IllegalStateException.class, v::verifyProductionConfiguration);
        assertTrue(ex.getMessage().contains("razorpay.enabled"));
    }

    @Test
    @DisplayName("an http:// public URL is rejected (payments require HTTPS)")
    void httpBaseUrlRejected() {
        ProductionValidator v = validator("http://futurebound.example", true,
                "rzp_live_ABC", "secret", "webhook", false, "", "jdbc:mysql://localhost:3306/fbt");
        IllegalStateException ex =
                assertThrows(IllegalStateException.class, v::verifyProductionConfiguration);
        assertTrue(ex.getMessage().contains("https://"));
    }

    @Test
    @DisplayName("bootstrap seeding with the demo admin password is rejected")
    void weakSeedAdminPasswordRejected() {
        ProductionValidator v = validator("https://futurebound.example", true,
                "rzp_live_ABC", "secret", "webhook", true, "password", "jdbc:mysql://localhost:3306/fbt");
        IllegalStateException ex =
                assertThrows(IllegalStateException.class, v::verifyProductionConfiguration);
        assertTrue(ex.getMessage().contains("ADMIN_SEED_PASSWORD"));
    }

    @Test
    @DisplayName("accidental H2 datasource under prod is rejected")
    void h2DatasourceRejected() {
        ProductionValidator v = validator("https://futurebound.example", true,
                "rzp_live_ABC", "secret", "webhook", false, "",
                "jdbc:h2:file:./data/future_bound_tech");
        IllegalStateException ex =
                assertThrows(IllegalStateException.class, v::verifyProductionConfiguration);
        assertTrue(ex.getMessage().contains("H2"));
    }

    @Test
    @DisplayName("all problems are reported together in one failure")
    void aggregatesEveryProblem() {
        ProductionValidator v = validator("http://x", false,
                "rzp_test_placeholder", "placeholder_secret", "placeholder_webhook_secret",
                true, "password", "jdbc:h2:mem:x");
        IllegalStateException ex =
                assertThrows(IllegalStateException.class, v::verifyProductionConfiguration);
        String msg = ex.getMessage();
        assertTrue(msg.contains("razorpay.enabled"));
        assertTrue(msg.contains("RAZORPAY_KEY_ID"));
        assertTrue(msg.contains("APP_BASE_URL"));
        assertTrue(msg.contains("ADMIN_SEED_PASSWORD"));
        assertTrue(msg.contains("H2"));
    }
}
