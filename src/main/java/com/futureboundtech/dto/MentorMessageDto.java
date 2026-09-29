package com.futureboundtech.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/** A single Future Mentor chat turn, as returned to the browser. */
@Data
@Accessors(chain = true)
public class MentorMessageDto {

    /** "user" or "assistant". */
    private String role;
    private String content;
    /** True when this assistant turn came from a real AI provider (not the fallback). */
    private boolean aiGenerated;
    private LocalDateTime createdAt;

    public static MentorMessageDto of(String role, String content, boolean aiGenerated, LocalDateTime createdAt) {
        return new MentorMessageDto()
                .setRole(role)
                .setContent(content)
                .setAiGenerated(aiGenerated)
                .setCreatedAt(createdAt);
    }
}
