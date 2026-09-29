package com.futureboundtech.enums;

/**
 * Publication state of an assignment. Students only ever see {@link #PUBLISHED}
 * assignments; a {@link #DRAFT} is editable by the trainer but not surfaced.
 */
public enum AssignmentStatus {
    DRAFT,
    PUBLISHED
}
