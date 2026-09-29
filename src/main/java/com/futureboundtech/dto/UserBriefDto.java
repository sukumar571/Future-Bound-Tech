package com.futureboundtech.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/** Compact user row used in dashboard widgets (recent registrations). */
@Data
@Accessors(chain = true)
public class UserBriefDto {
    private Long id;
    private String fullName;
    private String email;
    private String role;
    private String roleLabel;
    private boolean active;
    private LocalDateTime createdAt;
}
