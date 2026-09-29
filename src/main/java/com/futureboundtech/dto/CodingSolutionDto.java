package com.futureboundtech.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * A student's saved working solution for a coding problem, shown in the practice
 * workspace so they can pick up where they left off. Never executed by the app.
 */
@Data
@Accessors(chain = true)
public class CodingSolutionDto {
    private Long questionId;
    private String code;
    private String language;
    private boolean solved;
    private LocalDateTime lastSavedAt;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm");

    public boolean isPresent() {
        return lastSavedAt != null;
    }

    /** Human label for "last saved" or an empty-state hint. */
    public String getSavedLabel() {
        return lastSavedAt == null ? null : lastSavedAt.format(FMT);
    }
}
