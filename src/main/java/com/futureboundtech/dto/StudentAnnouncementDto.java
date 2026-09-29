package com.futureboundtech.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class StudentAnnouncementDto {

    private Long id;
    private String title;
    private String content;
    private String scope;      // batch name, or "Institute-wide"
    private LocalDateTime createdAt;
}
