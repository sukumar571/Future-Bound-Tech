package com.futureboundtech.api;

import com.futureboundtech.dto.UserRegistrationDto;
import com.futureboundtech.dto.api.ApiResponse;
import com.futureboundtech.dto.api.LoginRequest;
import com.futureboundtech.dto.api.RegisterRequest;
import com.futureboundtech.entity.User;
import com.futureboundtech.security.CustomUserDetails;
import com.futureboundtech.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Session-cookie authentication for the REST layer. A successful login stores the
 * {@code SecurityContext} in the HTTP session, so the returned {@code JSESSIONID}
 * cookie authenticates subsequent {@code /api/**} calls exactly like the browser
 * login does for the Thymeleaf UI.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthApiController {

    private final AuthService authService;
    private final AuthenticationConfiguration authenticationConfiguration;

    @SuppressWarnings("deprecation")
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Map<String, Object>>> register(@Valid @RequestBody RegisterRequest request) {
        UserRegistrationDto dto = new UserRegistrationDto();
        String[] name = splitName(request.getFullName());
        dto.setFirstName(name[0]);
        dto.setLastName(name[1]);
        dto.setEmail(request.getEmail());
        dto.setPhone(request.getPhone());
        dto.setPassword(request.getPassword());
        dto.setConfirmPassword(request.getPassword());
        try {
            authService.registerStudent(dto);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error(ex.getMessage() == null ? "Registration failed." : ex.getMessage()));
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("email", request.getEmail());
        data.put("role", "STUDENT");
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Account created", data));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(@Valid @RequestBody LoginRequest request,
                                                                  HttpServletRequest httpRequest,
                                                                  HttpServletResponse httpResponse) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                request.getUsername(), request.getPassword());
        try {
            Authentication manager = authenticationConfiguration.getAuthenticationManager().authenticate(authentication);
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(manager);
            SecurityContextHolder.setContext(context);
            securityContextRepository.saveContext(context, httpRequest, httpResponse);

            Object principal = manager.getPrincipal();
            Map<String, Object> data = principal instanceof CustomUserDetails cud
                    ? summary(cud.getUser())
                    : Map.of("email", request.getUsername());
            return ResponseEntity.ok(ApiResponse.ok("Logged in", data));
        } catch (AuthenticationException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Invalid email or password."));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Unable to authenticate right now."));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request, HttpServletResponse response) {
        new SecurityContextLogoutHandler().logout(request, response, SecurityContextHolder.getContext().getAuthentication());
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(ApiResponse.ok("Logged out", null));
    }

    private Map<String, Object> summary(User user) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", user.getId());
        data.put("email", user.getEmail());
        data.put("firstName", user.getFirstName());
        data.put("lastName", user.getLastName());
        data.put("role", user.getRole() == null ? null : user.getRole().name());
        return data;
    }

    private String[] splitName(String fullName) {
        String value = fullName == null ? "" : fullName.trim();
        int space = value.indexOf(' ');
        if (space < 0) {
            return new String[]{value, " "};
        }
        return new String[]{value.substring(0, space), value.substring(space + 1)};
    }
}
