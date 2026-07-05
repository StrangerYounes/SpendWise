package com.corner.takecontrol.data.model;

public class LeaderboardEntry {

    private final String userId;
    private final String displayName;
    private final int completionPercent;

    public LeaderboardEntry(String userId, String displayName, int completionPercent) {
        this.userId = userId;
        this.displayName = displayName;
        this.completionPercent = completionPercent;
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
}
