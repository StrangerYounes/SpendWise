package com.corner.takecontrol.util;

public final class XpUtil {

    private static final int BASE_XP = 100;
    private static final double LEVEL_MULTIPLIER = 1.5;

    private XpUtil() {}

    public static int calculateLevel(long xp) {
        // Simple exponential level scaling: xp = BASE_XP * (level ^ LEVEL_MULTIPLIER)
        // level = (xp / BASE_XP) ^ (1 / LEVEL_MULTIPLIER)
        if (xp < BASE_XP) return 1;
        return (int) Math.pow((double) xp / BASE_XP, 1.0 / LEVEL_MULTIPLIER) + 1;
    }

    public static long getXpForLevel(int level) {
        if (level <= 1) return 0;
        return (long) (BASE_XP * Math.pow(level - 1, LEVEL_MULTIPLIER));
    }

    public static long getXpReward(String taskType, boolean isLate) {
        long reward = 50; // Base reward for any task
        if (!isLate) {
            reward += 20; // Bonus for on-time completion
        }
        return reward;
    }
}
