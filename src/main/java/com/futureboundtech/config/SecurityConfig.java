package com.futureboundtech.config;

import com.futureboundtech.api.ApiSecurityHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Strict-Transport-Security is off unless explicitly enabled (the prod
     * profile turns it on via APP_HSTS_ENABLED) — sending HSTS while serving
     * plain HTTP locally would lock the browser into HTTPS-only for a year.
     */
    @org.springframework.beans.factory.annotation.Value("${app.security.hsts-enabled:false}")
    private boolean hstsEnabled;


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/courses/*/enroll").authenticated()
                        // Public pages
                        .requestMatchers(
                                "/", "/home", "/courses", "/courses/**",
                                "/about", "/contact", "/placements", "/practice",
                                "/batches", "/trainers", "/trainers/**", "/faq",
                                "/testimonials", "/community",
                                "/privacy-policy", "/terms",
                                "/certificate/verify", "/certificate/verify/**",
                                "/payment/success", "/payment/failed")
                        .permitAll()
                        // Static resources. Only public upload sub-folders are open;
                        // private files are served via authenticated /files/** endpoints.
                        .requestMatchers(
                                "/css/**", "/js/**", "/images/**", "/fonts/**",
                                "/uploads/branding/**", "/uploads/courses/**",
                                "/uploads/trainers/**", "/uploads/avatars/**",
                                "/favicon.ico", "/h2-console/**")
                        .permitAll()
                        // Auth pages
                        .requestMatchers(
                                "/login", "/register", "/forgot-password", "/reset-password/**")
                        .permitAll()
                        // API endpoints (webhook is called server-to-server by Razorpay)
                        .requestMatchers("/api/health", "/api/webhooks/**", "/api/payments/webhook",
                                "/api/certificates/verify").permitAll()
                        // REST API — public reads and auth
                        .requestMatchers("/api/auth/**", "/api/courses", "/api/courses/**",
                                "/api/classes/upcoming").permitAll()
                        // REST API — role-scoped
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/student/**").hasRole("STUDENT")
                        .requestMatchers("/api/trainer/**").hasRole("TRAINER")
                        // REST API — any authenticated user (student-scoped in-controller)
                        .requestMatchers("/api/enrollments", "/api/enrollments/**",
                                "/api/payments/**").authenticated()
                        // Role-based access — trainers use dedicated /trainer/** routes only
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/trainer/**").hasRole("TRAINER")
                        .requestMatchers("/student/**").hasRole("STUDENT")
                        // Everything else requires authentication
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/dashboard", true)
                        .failureUrl("/login?error=true")
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll())
                .exceptionHandling(ex -> ex
                        // API surface: return 401/403 JSON (registered first so it wins for /api/**).
                        .defaultAuthenticationEntryPointFor(new ApiSecurityHandler(),
                                new AntPathRequestMatcher("/api/**"))
                        // Browser surface: unauthenticated HTML is redirected to the login page.
                        .defaultAuthenticationEntryPointFor(new LoginUrlAuthenticationEntryPoint("/login"),
                                new AntPathRequestMatcher("/**"))
                        .accessDeniedHandler(new ApiSecurityHandler()));

        // Allow H2 console in dev
        http.csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**", "/api/**"));
        // Load the deferred CSRF token early so large first-hit form pages never hit
        // the "cannot create a session after response committed" error.
        http.addFilterAfter(new CsrfTokenEagerLoadFilter(), CsrfFilter.class);
        http.headers(headers -> {
            headers.frameOptions(frame -> frame.sameOrigin());
            if (hstsEnabled) {
                // Only meaningful behind HTTPS; ask browsers to stay on HTTPS for a year
                // and cover subdomains. Preload is left to the operator (see DEPLOYMENT.md).
                headers.httpStrictTransportSecurity(hsts -> hsts
                        .includeSubDomains(true)
                        .maxAgeInSeconds(31536000));
            }
        });

        return http.build();
    }
}
