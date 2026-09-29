package com.futureboundtech.enums;

public enum PaymentStatus {
    CREATED,
    PENDING,
    SUCCESS,
    FAILED,
    REFUNDED,
    CANCELLED;

    /**
     * A payment in this state represents money that has been confirmed by the
     * gateway (server-side) and can be used to activate an enrollment.
     */
    public boolean isSettled() {
        return this == SUCCESS;
    }
}
