package com.futureboundtech.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Whether a batch is accepting new enrollments. */
@Getter
@RequiredArgsConstructor
public enum BatchEnrollmentStatus {
    OPEN("Open"),
    CLOSED("Closed");

    private final String label;
}
