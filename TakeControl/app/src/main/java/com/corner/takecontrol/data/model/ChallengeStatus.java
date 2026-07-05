package com.corner.takecontrol.data.model;

public enum ChallengeStatus {
    DRAFT("draft"),
    ACTIVE("active"),
    COMPLETED("completed");

    private final String value;

    ChallengeStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ChallengeStatus fromValue(String value) {
        if (value == null) {
            return DRAFT;
        }
        for (ChallengeStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        return DRAFT;
    }
}
