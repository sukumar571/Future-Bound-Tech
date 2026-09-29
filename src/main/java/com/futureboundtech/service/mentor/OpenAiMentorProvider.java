package com.futureboundtech.service.mentor;

import com.futureboundtech.config.FutureMentorProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Optional network provider that calls an OpenAI-compatible {@code /chat/completions}
 * endpoint. It is only {@link #isAvailable() available} when AI has been explicitly
 * switched on and an API key has been supplied through the environment — the key is
 * never hard-coded.
 *
 * <p>Failures (timeouts, non-2xx, unexpected payloads) are surfaced as
 * {@link RuntimeException}s; the calling {@code FutureMentorService} catches them and
 * transparently falls back to the local assistant, so the LMS never breaks because
 * the AI is unreachable.</p>
 */
@Component
public class OpenAiMentorProvider implements AiAssistantProvider {

    public static final String ID = "openai";

    private final FutureMentorProperties properties;
    private final RestTemplate restTemplate;

    public OpenAiMentorProvider(FutureMentorProperties properties) {
        this.properties = properties;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        int millis = Math.max(1, properties.getTimeoutSeconds()) * 1000;
        factory.setConnectTimeout(millis);
        factory.setReadTimeout(millis);
        this.restTemplate = new RestTemplate(factory);
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public boolean isAvailable() {
        // "openai" must be the selected provider AND a key must be present.
        return properties.isAiUsable()
                && properties.getProvider() != null
                && ID.equalsIgnoreCase(properties.getProvider().trim());
    }

    @Override
    @SuppressWarnings("unchecked")
    public MentorReply answer(MentorPrompt prompt) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", prompt.systemInstructions()));
        if (prompt.learnerContext() != null && !prompt.learnerContext().isBlank()) {
            messages.add(Map.of("role", "system", "content", prompt.learnerContext()));
        }
        if (prompt.history() != null) {
            for (Turn t : prompt.history()) {
                if (t.content() != null && !t.content().isBlank()) {
                    messages.add(Map.of("role", t.role(), "content", t.content()));
                }
            }
        }
        messages.add(Map.of("role", "user", "content", prompt.question()));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", properties.getModel());
        body.put("messages", messages);
        body.put("max_tokens", properties.getMaxTokens());
        body.put("temperature", properties.getTemperature());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(properties.getApiKey());

        String url = properties.getBaseUrl().replaceAll("/+$", "") + "/chat/completions";
        Map<String, Object> response =
                restTemplate.postForObject(url, new HttpEntity<>(body, headers), Map.class);

        String content = extractContent(response);
        if (content == null || content.isBlank()) {
            throw new IllegalStateException("Empty completion from AI provider.");
        }
        return new MentorReply(content.trim(), true, ID);
    }

    @SuppressWarnings("unchecked")
    private String extractContent(Map<String, Object> response) {
        if (response == null) {
            return null;
        }
        Object choices = response.get("choices");
        if (choices instanceof List<?> list && !list.isEmpty()) {
            Object first = list.get(0);
            if (first instanceof Map<?, ?> choiceMap) {
                Object message = choiceMap.get("message");
                if (message instanceof Map<?, ?> msgMap) {
                    Object text = msgMap.get("content");
                    return text == null ? null : text.toString();
                }
            }
        }
        return null;
    }
}
