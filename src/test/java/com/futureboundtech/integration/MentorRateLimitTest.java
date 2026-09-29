package com.futureboundtech.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.TestPropertySource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the per-student Future Mentor throttle. The rate limit is tightened to
 * 2 requests/minute for this class only, so the third chat call must be rejected
 * with HTTP 429 (protecting the app from abuse and capping optional AI spend).
 */
@TestPropertySource(properties = "future-mentor.rate-limit.requests-per-minute=2")
class MentorRateLimitTest extends AbstractIntegrationTest {

    private static final String Q = "{\"message\":\"explain sql joins\"}";

    @Test
    @DisplayName("the third chat in a minute is rate limited with 429")
    void thirdRequestIsThrottled() throws Exception {
        seedStudent("mentor-rate@test.com", "password123");
        MockHttpSession session = login("mentor-rate@test.com", "password123");

        mockMvc.perform(post("/api/student/mentor/chat").session(session)
                        .contentType(MediaType.APPLICATION_JSON).content(Q))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/student/mentor/chat").session(session)
                        .contentType(MediaType.APPLICATION_JSON).content(Q))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/student/mentor/chat").session(session)
                        .contentType(MediaType.APPLICATION_JSON).content(Q))
                .andExpect(status().isTooManyRequests());
    }
}
