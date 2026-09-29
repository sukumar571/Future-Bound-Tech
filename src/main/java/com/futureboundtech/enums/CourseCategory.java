package com.futureboundtech.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CourseCategory {
    FULL_STACK("Full Stack"),
    WEB_DEVELOPMENT("Web Development"),
    CLOUD_COMPUTING("Cloud Computing"),
    ARTIFICIAL_INTELLIGENCE("Artificial Intelligence");

    private final String label;
}
