package com.corner.takecontrol.data.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;

public class TaskProgress {

    @DocumentId
    private String id;
    private String userId;
    private String taskId;
    private String periodKey;
    private double value;
    private boolean completed;

    @ServerTimestamp
    private Timestamp updatedAt;

    public TaskProgress() {
    }

    public TaskProgress(String userId, String taskId, String periodKey, double value, boolean completed) {
        this.userId = userId;
        this.taskId = taskId;
        this.periodKey = periodKey;
        this.value = value;
        this.completed = completed;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getPeriodKey() {
        return periodKey;
    }

    public void setPeriodKey(String periodKey) {
        this.periodKey = periodKey;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    public static String buildDocumentId(String userId, String taskId, String periodKey) {
        return userId + "_" + taskId + "_" + periodKey;
    }
}
