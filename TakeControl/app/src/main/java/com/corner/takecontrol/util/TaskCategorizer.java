package com.corner.takecontrol.util;

import java.util.HashMap;
import java.util.Map;

public class TaskCategorizer {
    private static final Map<String, String[]> CATEGORIES = new HashMap<>();
    private static final String[] ACTIONS = {
            "read", "memorize", "revise", "pray", "fast", "zakat", "dhikr", "work", "gym", "study"
    };

    static {
        // Minimal core defaults
        CATEGORIES.put("Fitness", new String[]{"walk", "run", "lift", "gym", "workout", "exercise"});
        CATEGORIES.put("Knowledge", new String[]{"read", "memorize", "learn", "study", "book"});
        CATEGORIES.put("Health", new String[]{"meditate", "yoga", "sleep", "journal", "water"});
        CATEGORIES.put("Productivity", new String[]{"work", "meeting", "call", "email", "plan"});
        CATEGORIES.put("Spiritual", new String[]{"quran", "surat", "younes", "pray", "fast", "zakat"});
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

    public static String extractAction(String title) {
        if (title == null) return "Unknown";
        String lower = title.toLowerCase();
        for (String action : ACTIONS) {
            if (lower.contains(action)) {
                return action.substring(0, 1).toUpperCase() + action.substring(1);
            }
        }
        String[] words = title.trim().split("\\s+");
        if (words.length > 0 && words[0].length() > 2) {
             return words[0].substring(0, 1).toUpperCase() + words[0].substring(1).toLowerCase();
        }
        return "Action";
    }
}
