package com.corner.takecontrol.util;

import com.corner.takecontrol.data.model.UserProfile;
import java.util.ArrayList;
import java.util.List;

public final class ProgressionUtil {

    private ProgressionUtil() {}

    public static int getMaxChallengeSlots(UserProfile profile) {
        if (profile.isUnlimitedChallengeSlots()) {
            return Integer.MAX_VALUE;
        }
        int level = profile.getLevel();
        if (level >= 35) return 6;
        if (level >= 20) return 5;
        if (level >= 10) return 4;
        return 3;
    }

    public static int getNextSlotLevel(int currentLevel) {
        if (currentLevel < 10) return 10;
        if (currentLevel < 20) return 20;
        if (currentLevel < 35) return 35;
        return -1; // No more slots to unlock
    }

    public static boolean checkAndUnlockRewards(UserProfile profile) {
        boolean updated = false;
        int level = profile.getLevel();

        // Check Frames
        if (level >= 5 && unlockFrame(profile, "BRONZE_FRAME")) updated = true;
        if (level >= 15 && unlockFrame(profile, "SILVER_FRAME")) updated = true;
        if (level >= 30 && unlockFrame(profile, "GOLD_FRAME")) updated = true;

        // Check Titles
        if (level >= 1 && unlockTitle(profile, "BEGINNER")) updated = true;
        if (level >= 10 && unlockTitle(profile, "DISCIPLINED")) updated = true;
        if (level >= 20 && unlockTitle(profile, "CONSISTENT")) updated = true;
        if (level >= 50 && unlockTitle(profile, "MASTER_OF_CONTROL")) updated = true;

        return updated;
    }

    private static boolean unlockFrame(UserProfile profile, String frameId) {
        if (profile.getUnlockedFrames() == null) {
            profile.setUnlockedFrames(new ArrayList<>());
        }
        if (!profile.getUnlockedFrames().contains(frameId)) {
            profile.getUnlockedFrames().add(frameId);
            return true;
        }
        return false;
    }

    private static boolean unlockTitle(UserProfile profile, String titleId) {
        if (profile.getUnlockedTitles() == null) {
            profile.setUnlockedTitles(new ArrayList<>());
        }
        if (!profile.getUnlockedTitles().contains(titleId)) {
            profile.getUnlockedTitles().add(titleId);
            return true;
        }
        return false;
    }

    public static List<CosmeticItem> getAllFrames() {
        List<CosmeticItem> items = new ArrayList<>();
        items.add(new CosmeticItem("NONE", "No Frame", 1, CosmeticType.FRAME));
        items.add(new CosmeticItem("BRONZE_FRAME", "Bronze Frame", 5, CosmeticType.FRAME));
        items.add(new CosmeticItem("SILVER_FRAME", "Silver Frame", 15, CosmeticType.FRAME));
        items.add(new CosmeticItem("GOLD_FRAME", "Gold Frame", 30, CosmeticType.FRAME));
        return items;
    }

    public static List<CosmeticItem> getAllTitles() {
        List<CosmeticItem> items = new ArrayList<>();
        items.add(new CosmeticItem("NONE", "No Title", 1, CosmeticType.TITLE));
        items.add(new CosmeticItem("BEGINNER", "Beginner", 1, CosmeticType.TITLE));
        items.add(new CosmeticItem("DISCIPLINED", "Disciplined", 10, CosmeticType.TITLE));
        items.add(new CosmeticItem("CONSISTENT", "Consistent", 20, CosmeticType.TITLE));
        items.add(new CosmeticItem("MASTER_OF_CONTROL", "Master of Control", 50, CosmeticType.TITLE));
        return items;
    }

    public static String getCosmeticName(String id) {
        if (id == null || id.equals("NONE")) return "";
        for (CosmeticItem item : getAllFrames()) {
            if (item.id.equals(id)) return item.name;
        }
        for (CosmeticItem item : getAllTitles()) {
            if (item.id.equals(id)) return item.name;
        }
        return id;
    }

    public static int getFrameColorRes(String frameId) {
        if (frameId == null) return com.corner.takecontrol.R.color.surface_variant;
        switch (frameId) {
            case "BRONZE_FRAME": return com.corner.takecontrol.R.color.rank_bronze;
            case "SILVER_FRAME": return com.corner.takecontrol.R.color.rank_silver;
            case "GOLD_FRAME": return com.corner.takecontrol.R.color.rank_gold;
            default: return com.corner.takecontrol.R.color.surface_variant;
        }
    }

    public enum CosmeticType {
        FRAME, TITLE
    }

    public static class CosmeticItem {
        public final String id;
        public final String name;
        public final int requiredLevel;
        public final CosmeticType type;

        public CosmeticItem(String id, String name, int requiredLevel, CosmeticType type) {
            this.id = id;
            this.name = name;
            this.requiredLevel = requiredLevel;
            this.type = type;
        }
    }

    public static class SlotStatus {
        public final int current;
        public final int max;
        public final int nextLevel;
        public final boolean unlimited;

        public SlotStatus(int current, int max, int nextLevel, boolean unlimited) {
            this.current = current;
            this.max = max;
            this.nextLevel = nextLevel;
            this.unlimited = unlimited;
        }
    }
}
