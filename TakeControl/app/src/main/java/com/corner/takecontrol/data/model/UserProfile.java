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
    // TODO: Switch to Firebase Storage for better efficiency if upgrading from the free tier.
    // This field stores the encrypted image bytes as a Base64 string directly in Firestore.
    private String encryptedPhoto;
    private int currentStreak;
    private int longestStreak;
    private String lastCompletionDate; // YYYY-MM-DD
    private long xp;
    private long weeklyXp;
    private long monthlyXp;
    private String lastXpUpdateWeek; // e.g. "2024-W50"
    private String lastXpUpdateMonth; // e.g. "2024-12"
    private int level;
    private List<String> achievements;
    private List<String> unlockedFrames;
    private List<String> unlockedTitles;
    private String equippedFrameId;
    private String equippedTitleId;
    private String country;
    private String university;
    private String company;
    private List<String> friendIds = new ArrayList<>();
    private List<String> sentRequestIds = new ArrayList<>();
    private String flexedRankScope; // Global, Country, University, Company
    private String flexedRankTimeframe; // xp, weeklyXp, monthlyXp
    private boolean unlimitedChallengeSlots;
    private List<String> customCategories = new ArrayList<>();
    private List<String> customActions = new ArrayList<>();

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

    public String getEncryptedPhoto() {
        return encryptedPhoto;
    }

    public void setEncryptedPhoto(String encryptedPhoto) {
        this.encryptedPhoto = encryptedPhoto;
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

    public long getWeeklyXp() {
        return weeklyXp;
    }

    public void setWeeklyXp(long weeklyXp) {
        this.weeklyXp = weeklyXp;
    }

    public long getMonthlyXp() {
        return monthlyXp;
    }

    public void setMonthlyXp(long monthlyXp) {
        this.monthlyXp = monthlyXp;
    }

    public String getLastXpUpdateWeek() {
        return lastXpUpdateWeek;
    }

    public void setLastXpUpdateWeek(String lastXpUpdateWeek) {
        this.lastXpUpdateWeek = lastXpUpdateWeek;
    }

    public String getLastXpUpdateMonth() {
        return lastXpUpdateMonth;
    }

    public void setLastXpUpdateMonth(String lastXpUpdateMonth) {
        this.lastXpUpdateMonth = lastXpUpdateMonth;
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

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getUniversity() {
        return university;
    }

    public void setUniversity(String university) {
        this.university = university;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public List<String> getFriendIds() {
        return friendIds;
    }

    public void setFriendIds(List<String> friendIds) {
        this.friendIds = friendIds;
    }

    public List<String> getSentRequestIds() {
        return sentRequestIds;
    }

    public void setSentRequestIds(List<String> sentRequestIds) {
        this.sentRequestIds = sentRequestIds;
    }

    public String getFlexedRankScope() {
        return flexedRankScope;
    }

    public void setFlexedRankScope(String flexedRankScope) {
        this.flexedRankScope = flexedRankScope;
    }

    public String getFlexedRankTimeframe() {
        return flexedRankTimeframe;
    }

    public void setFlexedRankTimeframe(String flexedRankTimeframe) {
        this.flexedRankTimeframe = flexedRankTimeframe;
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

    public List<String> getCustomCategories() {
        return customCategories;
    }

    public void setCustomCategories(List<String> customCategories) {
        this.customCategories = customCategories;
    }

    public List<String> getCustomActions() {
        return customActions;
    }

    public void setCustomActions(List<String> customActions) {
        this.customActions = customActions;
    }
}
