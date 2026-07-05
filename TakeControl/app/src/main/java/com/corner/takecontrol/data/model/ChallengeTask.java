package com.corner.takecontrol.data.model;

import com.google.firebase.firestore.DocumentId;

public class ChallengeTask {

    @DocumentId
    private String id;
    private String title;
    private String description;
    private String frequency;
    private String taskType;
    private double targetValue;
    private String unit;
    private int orderIndex;

    public ChallengeTask() {
    }

    public ChallengeTask(String title, String frequency, String taskType, double targetValue, String unit, int orderIndex) {
        this.title = title;
        this.frequency = frequency;
        this.taskType = taskType;
        this.targetValue = targetValue;
        this.unit = unit;
        this.orderIndex = orderIndex;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public TaskFrequency getFrequencyEnum() {
        return TaskFrequency.fromValue(frequency);
    }

    public void setFrequencyEnum(TaskFrequency frequency) {
        this.frequency = frequency.getValue();
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public TaskType getTaskTypeEnum() {
        return TaskType.fromValue(taskType);
    }

    public void setTaskTypeEnum(TaskType taskType) {
        this.taskType = taskType.getValue();
    }

    public double getTargetValue() {
        return targetValue;
    }

    public void setTargetValue(double targetValue) {
        this.targetValue = targetValue;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public int getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(int orderIndex) {
        this.orderIndex = orderIndex;
    }
}
