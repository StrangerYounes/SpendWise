package com.corner.takecontrol.data.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;

public class AppNotification {

    @DocumentId
    private String id;
    private String toUserId;
    private String fromUserId;
    private String fromUserName;
    private String type; // NUDGE, CHEER, ACHIEVEMENT
    private String message;
    private boolean read;

    @ServerTimestamp
    private Timestamp createdAt;

    public AppNotification() {}

    public AppNotification(String toUserId, String fromUserId, String fromUserName, String type, String message) {
        this.toUserId = toUserId;
        this.fromUserId = fromUserId;
        this.fromUserName = fromUserName;
        this.type = type;
        this.message = message;
        this.read = false;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getToUserId() {
        return toUserId;
    }

    public void setToUserId(String toUserId) {
        this.toUserId = toUserId;
    }

    public String getFromUserId() {
        return fromUserId;
    }

    public void setFromUserId(String fromUserId) {
        this.fromUserId = fromUserId;
    }

    public String getFromUserName() {
        return fromUserName;
    }

    public void setFromUserName(String fromUserName) {
        this.fromUserName = fromUserName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
