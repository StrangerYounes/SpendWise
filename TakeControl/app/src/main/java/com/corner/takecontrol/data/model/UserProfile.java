package com.corner.takecontrol.data.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.ArrayList;
import java.util.List;

public class UserProfile {

    @DocumentId
    private String id;
    private String displayName;
    private String photoUrl;
    private int currentStreak;
    private int longestStreak;
    private String lastCompletionDate; // YYYY-MM-DD
    private long xp;
    private int level;
    private List<String> achievements;
    private List<String> unlockedFrames;
    private List<String> unlockedTitles;
    private String equippedFrameId;
    private String equippedTitleId;
    private boolean unlimitedChallengeSlots;

    @ServerTimestamp
    private Timestamp createdAt;

    public UserProfile() {
        this.achievements = new ArrayList<>();
        this.unlockedFrames = new ArrayList<>();
        this.unlockedTitles = new ArrayList<>();
    }

    public UserProfile(String id, String displayName) {
        this();
        this.id = id;
        this.displayName = displayName;
        this.unlockedTitles.add("BEGINNER");
        this.equippedTitleId = "BEGINNER";
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public int getCurrentStreak() {
        return currentStreak;
    }

    public void setCurrentStreak(int currentStreak) {
        this.currentStreak = currentStreak;
    }

    public int getLongestStreak() {
        return longestStreak;
    }

    public void setLongestStreak(int longestStreak) {
        this.longestStreak = longestStreak;
    }

    public String getLastCompletionDate() {
        return lastCompletionDate;
    }

    public void setLastCompletionDate(String lastCompletionDate) {
        this.lastCompletionDate = lastCompletionDate;
    }

    public long getXp() {
        return xp;
    }

    public void setXp(long xp) {
        this.xp = xp;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public List<String> getAchievements() {
        return achievements;
    }

    public void setAchievements(List<String> achievements) {
        this.achievements = achievements;
    }

    public List<String> getUnlockedFrames() {
        return unlockedFrames;
    }

    public void setUnlockedFrames(List<String> unlockedFrames) {
        this.unlockedFrames = unlockedFrames;
    }

    public List<String> getUnlockedTitles() {
        return unlockedTitles;
    }

    public void setUnlockedTitles(List<String> unlockedTitles) {
        this.unlockedTitles = unlockedTitles;
    }

    public String getEquippedFrameId() {
        return equippedFrameId;
    }

    public void setEquippedFrameId(String equippedFrameId) {
        this.equippedFrameId = equippedFrameId;
    }

    public String getEquippedTitleId() {
        return equippedTitleId;
    }

    public void setEquippedTitleId(String equippedTitleId) {
        this.equippedTitleId = equippedTitleId;
    }

    public boolean isUnlimitedChallengeSlots() {
        return unlimitedChallengeSlots;
    }

    public void setUnlimitedChallengeSlots(boolean unlimitedChallengeSlots) {
        this.unlimitedChallengeSlots = unlimitedChallengeSlots;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
