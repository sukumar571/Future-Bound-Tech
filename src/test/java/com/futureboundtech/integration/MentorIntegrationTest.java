package com.futureboundtech.integration;

import com.futureboundtech.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the Future Mentor API graph end to end against the real service beans.
 *
 * <p>AI is left OFF (the default), so every answer must come from the safe offline
 * study guide — proving the assistant works with no external dependency and never
 * claims certainty. Access is always scoped to the signed-in student.</p>
 */
class MentorIntegrationTest extends AbstractIntegrationTest {

    private static final String JAVA_QUESTION = "{\"message\":\"explain java interfaces\"}";

    @Test
    @DisplayName("with no AI configured the assistant answers from the offline guide and flags it as not AI-generated")
    void chatFallsBackToOfflineGuide() throws Exception {
        User student = seedStudent("mentor-chat@test.com", "password123");
        MockHttpSession session = login("mentor-chat@test.com", "password123");

        mockMvc.perform(post("/api/student/mentor/chat").session(session)
                        .contentType(MediaType.APPLICATION_JSON).content(JAVA_QUESTION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.aiConfigured").value(false))
                .andExpect(jsonPath("$.data.aiGenerated").value(false))
                .andExpect(jsonPath("$.data.reply.content").value(containsString("Java pointers")))
                .andExpect(jsonPath("$.data.reply.content").value(containsString("verify")));
    }

    @Test
    @DisplayName("an empty question is rejected with 400")
    void blankQuestionRejected() throws Exception {
        seedStudent("mentor-blank@test.com", "password123");
        MockHttpSession session = login("mentor-blank@test.com", "password123");

        mockMvc.perform(post("/api/student/mentor/chat").session(session)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"   \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("chat history persists the turn when history is enabled, and clears on request")
    void historyPersistsAndClears() throws Exception {
        seedStudent("mentor-history@test.com", "password123");
        MockHttpSession session = login("mentor-history@test.com", "password123");

        mockMvc.perform(post("/api/student/mentor/chat").session(session)
                        .contentType(MediaType.APPLICATION_JSON).content(JAVA_QUESTION))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/student/mentor/history").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[?(@.role=='user')].content").value(hasItem(containsString("java"))))
                .andExpect(jsonPath("$.data[?(@.role=='assistant')].content").value(hasItem(containsString("Java pointers"))));

        mockMvc.perform(delete("/api/student/mentor/history").session(session))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/student/mentor/history").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("unauthenticated API chat calls are refused with 401 (never an HTML redirect)")
    void unauthenticatedRejected() throws Exception {
        mockMvc.perform(post("/api/student/mentor/chat")
                        .contentType(MediaType.APPLICATION_JSON).content(JAVA_QUESTION))
                .andExpect(status().isUnauthorized());
    }
}
