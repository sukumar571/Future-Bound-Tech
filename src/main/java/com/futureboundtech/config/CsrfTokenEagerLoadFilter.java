package com.futureboundtech.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Forces the deferred CSRF token to be loaded (and persisted to the session) early in
 * the filter chain, before the view starts writing output. Without this, a page whose
 * rendered size crosses the servlet response commit threshold before its CSRF form is
 * written can fail with "Cannot create a session after the response has been committed"
 * on a brand-new (cookie-less) request.
 */
public class CsrfTokenEagerLoadFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Object token = request.getAttribute(CsrfToken.class.getName());
        if (token instanceof CsrfToken csrfToken) {
            // Calling getToken() loads the token from the repository and, for the
            // session-based repository, creates the HttpSession while it is still safe.
            csrfToken.getToken();
        }
        filterChain.doFilter(request, response);
    }
}
