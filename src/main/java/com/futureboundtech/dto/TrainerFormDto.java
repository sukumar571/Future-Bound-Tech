package com.futureboundtech.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.web.multipart.MultipartFile;

/** Create/edit form backing object for trainer accounts (admin portal). */
@Data
@Accessors(chain = true)
public class TrainerFormDto {

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

    private String password;
    private String confirmPassword;

    private String expertise;

    private Integer experienceYears;

    @Size(max = 1000, message = "Bio is too long")
    private String bio;

    /** Current stored photo path/URL, used for the edit-screen preview. */
    private String photoUrl;

    /** Newly uploaded photo; null when the admin does not change the picture. */
    private MultipartFile photoFile;

    private boolean active = true;
}
