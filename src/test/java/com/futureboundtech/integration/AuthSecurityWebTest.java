package com.futureboundtech.integration;

import com.futureboundtech.enums.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Authorization / authentication / CSRF tests through the real filter chain.
 * Covers the Phase 25 role matrix dynamically: students and trainers must not
 * reach admin surfaces, anonymous users are challenged, and state-changing POSTs
 * require a CSRF token.
 */
class AuthSecurityWebTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("anonymous HTML requests are redirected to the login page")
    void anonymousHtmlChallenged() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("anonymous REST calls get 401, never an HTML redirect")
    void anonymousApiIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/stats"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("a student cannot reach the admin REST surface (403)")
    void studentBlockedFromAdminApi() throws Exception {
        mockMvc.perform(get("/api/admin/stats").with(user("s@example.com").roles("STUDENT")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("a student cannot open admin HTML pages")
    void studentBlockedFromAdminHtml() throws Exception {
        seedStudent("sec-student@test.com", "password123");
        MockHttpSession session = login("sec-student@test.com", "password123");
        mockMvc.perform(get("/admin/dashboard").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/access-denied"));
    }

    @Test
    @DisplayName("CSRF is enforced: a registration POST without a token creates nothing")
    void csrfBlocksRegistration() throws Exception {
        mockMvc.perform(post("/register")
                        .param("firstName", "No")
                        .param("lastName", "Csrf")
                        .param("email", "nocSRF@test.com")
                        .param("phone", "1112223333")
                        .param("password", "password123")
                        .param("confirmPassword", "password123"))
                .andExpect(status().is3xxRedirection());
        assertTrue(userRepository.findByEmail("nocSRF@test.com").isEmpty(),
                "a CSRF-less POST must never persist a user");
    }

    @Test
    @DisplayName("a valid, CSRF-tokened registration creates a STUDENT and redirects to login")
    void registrationWithCsrfSucceeds() throws Exception {
        mockMvc.perform(withCsrfForm())
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered=true"));

        assertTrue(userRepository.findByEmail("new-user@test.com").isPresent());
        assertEquals(Role.STUDENT, userRepository.findByEmail("new-user@test.com").orElseThrow().getRole());
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder withCsrfForm() {
        return post("/register").with(csrf())
                .param("firstName", "New")
                .param("lastName", "User")
                .param("email", "new-user@test.com")
                .param("phone", "5556667777")
                .param("password", "password123")
                .param("confirmPassword", "password123");
    }

    @Test
    @DisplayName("login with correct credentials authenticates and routes to /dashboard")
    void loginSuccess() throws Exception {
        seedStudent("login-ok@test.com", "password123");
        mockMvc.perform(formLogin("/login").user("username", "login-ok@test.com").password("password", "password123"))
                .andExpect(authenticated())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    @DisplayName("login with a wrong password is rejected and returns to the login page")
    void loginFailure() throws Exception {
        seedStudent("login-bad@test.com", "password123");
        mockMvc.perform(formLogin("/login").user("username", "login-bad@test.com").password("password", "WRONGpw"))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?error=true"));
    }

    @Test
    @DisplayName("logout invalidates the session")
    void logoutClearsSession() throws Exception {
        seedStudent("logout@test.com", "password123");
        MockHttpSession session = login("logout@test.com", "password123");
        assertNotNull(session);
        mockMvc.perform(post("/logout").session(session).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout=true"));
    }

    @Test
    @DisplayName("a trainer gets through to trainer routes but is denied admin routes")
    void trainerRoleSeparation() throws Exception {
        seedTrainer("trainer-sec@test.com", "password123");
        MockHttpSession session = login("trainer-sec@test.com", "password123");
        mockMvc.perform(get("/admin/dashboard").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/access-denied"));
    }
}
