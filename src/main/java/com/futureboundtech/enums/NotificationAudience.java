package com.futureboundtech.enums;

import lombok.Getter;

/** Who a manually sent notification is delivered to. */
@Getter
public enum NotificationAudience {

    ALL_STUDENTS("All students"),
    COURSE("Students of a course"),
    BATCH("Students of a batch"),
    STUDENT("A single student"),
    ALL_TRAINERS("All trainers"),
    TRAINER("A single trainer");

    private final String label;

    NotificationAudience(String label) {
        this.label = label;
    }
}
