package com.futureboundtech.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * Self-service profile edit form for a signed-in trainer. Deliberately limited to the fields a
 * trainer may change (name, phone, expertise, bio). Role, trainer id, active status, credentials
 * and payment/authorisation details are intentionally absent so they cannot be bound or tampered
 * with from the trainer form.
 */
@Data
@Accessors(chain = true)
public class TrainerProfileFormDto {

    @NotBlank(message = "First name is required")
    @Size(max = 60, message = "First name is too long")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 60, message = "Last name is too long")
    private String lastName;

    @Size(max = 20, message = "Phone number is too long")
    @Pattern(regexp = "^$|^[+0-9].{5,19}$", message = "Enter a valid phone number")
    private String phone;

    @Size(max = 160, message = "Expertise is too long")
    private String expertise;

    @Size(max = 1000, message = "Bio is too long")
    private String bio;

    // Read-only display values (never persisted from this form).
    private String email;
}
