package com.futureboundtech.dto;

import com.futureboundtech.enums.NotificationRelatedType;
import com.futureboundtech.enums.NotificationType;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * Read model for one inbox row, shared by the student, trainer and admin views.
 * Carries the presentation hints (icon / badge class / action link) so templates
 * never map notification types by hand.
 */
@Data
@Accessors(chain = true)
public class NotificationDto {

    private Long id;
    private String title;
    private String message;
    private NotificationType type;
    private String typeLabel;
    private String icon;
    private String badgeClass;
    private NotificationRelatedType relatedType;
    private Long relatedId;
    /** Role-scoped page that answers this notification; null when there is nothing to open. */
    private String actionUrl;
    /** Link text for {@link #actionUrl}, e.g. "Open payments". */
    private String actionLabel;
    private boolean read;
    private LocalDateTime createdAt;

    /** Recipient details, only populated on admin oversight screens. */
    private String recipientName;
    private String recipientEmail;

    /** Falls back to the type label so a notification never renders a blank heading. */
    public String getDisplayTitle() {
        return title == null || title.isBlank() ? typeLabel : title;
    }
}
