package com.futureboundtech.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Builder;
import jakarta.validation.constraints.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "auditlogs")
public class AuditLog extends BaseEntity {

    private String action;
    
    @NotBlank
    @Column(nullable = false)
    private String username;
    
    @Size(max = 2000)
    @Column(length = 2000)
    private String details;
    
    private String ipAddress;

}
