package com.corner.takecontrol.data.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.ArrayList;
import java.util.List;

public class Challenge {

    @DocumentId
    private String id;
    private String title;
    private String description;
    private String createdBy;
    private int durationDays;
    private Timestamp startDate;
    private Timestamp endDate;
    private String status;
    private String shareCode;
    private List<String> memberIds = new ArrayList<>();
    private List<String> archivedMemberIds = new ArrayList<>();
    private int maxSkips;
    private java.util.Map<String, Integer> memberSkips = new java.util.HashMap<>();

    @ServerTimestamp
    private Timestamp createdAt;

    public Challenge() {
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

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public int getDurationDays() {
        return durationDays;
    }

    public void setDurationDays(int durationDays) {
        this.durationDays = durationDays;
    }

    public Timestamp getStartDate() {
        return startDate;
    }

    public void setStartDate(Timestamp startDate) {
        this.startDate = startDate;
    }

    public Timestamp getEndDate() {
        return endDate;
    }

    public void setEndDate(Timestamp endDate) {
        this.endDate = endDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public ChallengeStatus getStatusEnum() {
        return ChallengeStatus.fromValue(status);
    }

    public void setStatusEnum(ChallengeStatus status) {
        this.status = status.getValue();
    }

    public String getShareCode() {
        return shareCode;
    }

    public void setShareCode(String shareCode) {
        this.shareCode = shareCode;
    }

    public List<String> getMemberIds() {
        return memberIds;
    }

    public void setMemberIds(List<String> memberIds) {
        this.memberIds = memberIds;
    }

    public List<String> getArchivedMemberIds() {
        return archivedMemberIds;
    }

    public void setArchivedMemberIds(List<String> archivedMemberIds) {
        this.archivedMemberIds = archivedMemberIds;
    }

    public int getMaxSkips() {
        return maxSkips;
    }

    public void setMaxSkips(int maxSkips) {
        this.maxSkips = maxSkips;
    }

    public java.util.Map<String, Integer> getMemberSkips() {
        return memberSkips;
    }

    public void setMemberSkips(java.util.Map<String, Integer> memberSkips) {
        this.memberSkips = memberSkips;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public int getMemberCount() {
        return memberIds != null ? memberIds.size() : 0;
    }
}
