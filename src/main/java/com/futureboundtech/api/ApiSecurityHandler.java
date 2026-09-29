package com.futureboundtech.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;

/**
 * Gives the {@code /api/**} surface correct JSON status codes without disturbing
 * the Thymeleaf browser flow: unauthenticated API calls get 401, forbidden ones
 * 403, while non-API requests keep the normal login/access-denied redirects.
 */
public class ApiSecurityHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        writeJson(response, HttpServletResponse.SC_UNAUTHORIZED,
                "Authentication required. Log in via POST /api/auth/login.");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        String path = request.getRequestURI();
        if (path != null && path.startsWith("/api/")) {
            writeJson(response, HttpServletResponse.SC_FORBIDDEN,
                    "You do not have permission to perform this action.");
        } else {
            response.sendRedirect(request.getContextPath() + "/access-denied");
        }
    }

    private void writeJson(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"success\":false,\"message\":\"" + message + "\"}");
    }
}
