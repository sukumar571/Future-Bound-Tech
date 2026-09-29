package com.futureboundtech.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Delivery mode for a scheduled live class. */
@Getter
@RequiredArgsConstructor
public enum ClassMode {
    ONLINE("Online"),
    OFFLINE("Offline");

    private final String label;
}
