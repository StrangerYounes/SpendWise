package com.corner.takecontrol.data.model;

public class LeaderboardEntry {

    private final String userId;
    private final String displayName;
    private final int completionPercent;
    private final String photoUrl;
    private final String equippedFrameId;
    private final String equippedTitleId;

    public LeaderboardEntry(String userId, String displayName, int completionPercent, String photoUrl, String equippedFrameId, String equippedTitleId) {
        this.userId = userId;
        this.displayName = displayName;
        this.completionPercent = completionPercent;
        this.photoUrl = photoUrl;
        this.equippedFrameId = equippedFrameId;
        this.equippedTitleId = equippedTitleId;
    }

    public String getUserId() {
        return userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getCompletionPercent() {
        return completionPercent;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public String getEquippedFrameId() {
        return equippedFrameId;
    }

    public String getEquippedTitleId() {
        return equippedTitleId;
    }
}
