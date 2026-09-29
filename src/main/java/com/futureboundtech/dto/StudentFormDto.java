package com.futureboundtech.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.experimental.Accessors;

/** Create/edit form backing object for student accounts (admin portal). */
@Data
@Accessors(chain = true)
public class StudentFormDto {

    private Long userId;

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    private String email;

    @NotBlank(message = "Phone number is required")
    private String phone;

    /** Required when creating; optional when editing (blank keeps the current password). */
    private String password;
    private String confirmPassword;

    private String education;
    private boolean active = true;
}
