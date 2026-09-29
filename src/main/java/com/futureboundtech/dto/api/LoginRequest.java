package com.futureboundtech.dto.api;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.experimental.Accessors;

/** Credential payload for {@code POST /api/auth/login}. */
@Data
@Accessors(chain = true)
public class LoginRequest {

    @NotBlank(message = "Email is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;
}
