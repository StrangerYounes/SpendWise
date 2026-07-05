package com.corner.takecontrol.data.model;

public enum TaskType {
    CHECKMARK("checkmark"),
    NUMERIC("numeric"),
    DURATION("duration");

    private final String value;

    TaskType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static TaskType fromValue(String value) {
        if (value == null) {
            return CHECKMARK;
        }
        for (TaskType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        return CHECKMARK;
    }
}
