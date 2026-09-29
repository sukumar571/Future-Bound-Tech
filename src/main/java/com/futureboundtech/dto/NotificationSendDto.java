package com.futureboundtech.dto;

import com.futureboundtech.enums.NotificationAudience;
import com.futureboundtech.enums.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * Form backing "admin sends a notification" and "trainer posts to a batch".
 * Which id is required depends on {@link #audience}; the service validates that
 * pairing so a malformed request can never silently broadcast to everyone.
 */
@Data
@Accessors(chain = true)
public class NotificationSendDto {

    @NotNull(message = "Choose who should receive this notification")
    private NotificationAudience audience = NotificationAudience.ALL_STUDENTS;

    /** Required for audience COURSE. */
    private Long courseId;

    /** Required for audience BATCH. */
    private Long batchId;

    /** Required for audience STUDENT. */
    private Long studentId;

    /** Required for audience TRAINER. */
    private Long trainerId;

    @NotNull(message = "Choose a notification type")
    private NotificationType type = NotificationType.GENERAL_ANNOUNCEMENT;

    @NotBlank(message = "Title is required")
    @Size(max = 160, message = "Title must be 160 characters or fewer")
    private String title;

    @NotBlank(message = "Message is required")
    @Size(max = 1000, message = "Message must be 1000 characters or fewer")
    private String message;

    /** True when this send targets trainer accounts rather than students. */
    public boolean trainerAudience() {
        return audience == NotificationAudience.ALL_TRAINERS || audience == NotificationAudience.TRAINER;
    }
}
