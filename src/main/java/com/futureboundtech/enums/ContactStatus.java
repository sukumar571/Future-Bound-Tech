package com.futureboundtech.enums;

import lombok.Getter;

/** Workflow status an admin assigns to a public contact message. */
@Getter
public enum ContactStatus {

    NEW("New"),
    FOLLOW_UP("Follow-up"),
    RESOLVED("Resolved");

    private final String label;

    ContactStatus(String label) {
        this.label = label;
    }
}
