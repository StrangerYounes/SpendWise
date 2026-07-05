package com.corner.takecontrol.data.model;

public enum TaskFrequency {
    DAILY("daily"),
    WEEKLY("weekly"),
    MONTHLY("monthly");

    private final String value;

    TaskFrequency(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static TaskFrequency fromValue(String value) {
        if (value == null) {
            return DAILY;
        }
        for (TaskFrequency frequency : values()) {
            if (frequency.value.equals(value)) {
                return frequency;
            }
        }
        return DAILY;
    }
}
