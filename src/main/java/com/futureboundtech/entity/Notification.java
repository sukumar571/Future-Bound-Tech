package com.futureboundtech.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Builder;
import com.futureboundtech.enums.NotificationRelatedType;
import com.futureboundtech.enums.NotificationType;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * A single message in one recipient's inbox. Fan-out creates one row per
 * recipient, which is what makes the per-recipient read / delete flags work.
 */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "notifications")
public class Notification extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

    @Size(max = 160)
    @Column(length = 160)
    private String title;

    @NotBlank
    @Column(nullable = false, length = 1000)
    private String message;

    @NotNull
    @Enumerated(EnumType.STRING)
    private NotificationType type = NotificationType.INFO;

    /** Optional pointer to the domain object this notification is about. */
    @Enumerated(EnumType.STRING)
    @Column(name = "related_type")
    private NotificationRelatedType relatedType;

    @Column(name = "related_id")
    private Long relatedId;

    private boolean isRead = false;

}
