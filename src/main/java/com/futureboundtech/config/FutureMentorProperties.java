package com.futureboundtech.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Binding for the optional "Future Mentor" learning assistant. Every value can be
 * driven through environment variables (see application.properties) so that no
 * secret — most importantly the AI provider API key — is ever committed to source
 * control. The whole feature is optional: when {@code aiEnabled} is false (the
 * default) or no key is present, the assistant falls back to a local, rules-based
 * study guide and the core LMS is unaffected.
 */
@Data
@Component
@ConfigurationProperties(prefix = "future-mentor")
public class FutureMentorProperties {

    /** Whether the assistant page and its endpoints are available at all. */
    private boolean enabled = true;

    /** Whether real AI provider calls are attempted. Requires a non-blank api-key. */
    private boolean aiEnabled = false;

    /** AI provider id. Only "openai" (OpenAI-compatible chat) is built in today. */
    private String provider = "openai";

    /** Provider API key. Never hard-coded; read from FUTURE_MENTOR_API_KEY. */
    private String apiKey = "";

    /** OpenAI-compatible base URL (chat completions are appended to it). */
    private String baseUrl = "https://api.openai.com/v1";

    private String model = "gpt-4o-mini";
    private int maxTokens = 500;
    private double temperature = 0.4;
    private int timeoutSeconds = 20;

    /** When false, conversations are answered live but never persisted. */
    private boolean historyEnabled = true;

    /** How many recent turns (user+assistant) are replayed to the model for context. */
    private int historyMaxTurnsSent = 10;

    private RateLimit rateLimit = new RateLimit();

    /** True only when AI is switched on AND a key is configured. */
    public boolean isAiUsable() {
        return enabled && aiEnabled && apiKey != null && !apiKey.isBlank();
    }

    @Data
    public static class RateLimit {
        /** Max questions a single student may ask per minute. */
        private int requestsPerMinute = 12;
        /** Hard cap on the length of one question, to bound prompt size / abuse. */
        private int maxInputChars = 2000;
    }
}
