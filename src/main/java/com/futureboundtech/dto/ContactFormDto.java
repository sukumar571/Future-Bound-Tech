package com.futureboundtech.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

/** Public "Contact us" form. Persisted as a ContactMessage by the website service. */
@Data
@Accessors(chain = true)
public class ContactFormDto {

    @NotBlank(message = "Please tell us your name")
    @Size(max = 120, message = "Name is too long")
    private String name;

    @NotBlank(message = "Please provide an email address")
    @Email(message = "Enter a valid email address")
    @Size(max = 160, message = "Email is too long")
    private String email;

    @Size(max = 40, message = "Phone number is too long")
    private String phone;

    @Size(max = 160, message = "Course interest is too long")
    private String courseInterest;

    @NotBlank(message = "Please write a message")
    @Size(max = 2000, message = "Message is too long")
    private String message;
}
