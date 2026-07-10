package com.corner.takecontrol.util;

import java.util.HashMap;
import java.util.Map;

public class TaskCategorizer {
    private static final Map<String, String[]> CATEGORIES = new HashMap<>();

    static {
        // Suggested keywords plus some commons
        CATEGORIES.put("Fitness", new String[]{"walk", "run", "lift", "gym", "train", "pushups", "workout", "km", "miles", "exercise", "swim", "bike"});
        CATEGORIES.put("Knowledge", new String[]{"read", "memorize", "learn", "study", "pages", "book", "course", "write", "practice"});
        CATEGORIES.put("Health", new String[]{"meditate", "yoga", "sleep", "journal", "water", "liters", "eat", "fruit", "vegetable"});
    }

    public static String categorize(String title) {
        if (title == null) return "Other";
        String lower = title.toLowerCase();
        for (Map.Entry<String, String[]> entry : CATEGORIES.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (lower.contains(keyword)) return entry.getKey();
            }
        }
        return "Other";
    }
}
