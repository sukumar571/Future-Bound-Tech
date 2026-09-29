package com.futureboundtech.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/** Contact message read model for the admin inbox. */
@Data
@Accessors(chain = true)
public class ContactMessageDto {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String courseInterest;
    private String subject;
    private String message;
    private boolean replied;
    private com.futureboundtech.enums.ContactStatus status;
    private String statusLabel;
    private LocalDateTime createdAt;
}
