package com.corner.takecontrol.data.model;

import com.google.firebase.firestore.DocumentId;
import java.util.HashMap;
import java.util.Map;

public class UserStats {
    @DocumentId
    private String userId;
    private int totalTasksCompleted;
    private Map<String, Double> unitTotals = new HashMap<>();
    private Map<String, Double> categoryTotals = new HashMap<>();
    private Map<String, Integer> dailyCompletions = new HashMap<>();
    private Map<String, Map<String, Double>> unitActionTotals = new HashMap<>();

    public UserStats() {
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getTotalTasksCompleted() {
        return totalTasksCompleted;
    }

    public void setTotalTasksCompleted(int totalTasksCompleted) {
        this.totalTasksCompleted = totalTasksCompleted;
    }

    public Map<String, Double> getUnitTotals() {
        return unitTotals;
    }

    public void setUnitTotals(Map<String, Double> unitTotals) {
        this.unitTotals = unitTotals;
    }

    public Map<String, Double> getCategoryTotals() {
        return categoryTotals;
    }

    public void setCategoryTotals(Map<String, Double> categoryTotals) {
        this.categoryTotals = categoryTotals;
    }

    public Map<String, Integer> getDailyCompletions() {
        return dailyCompletions;
    }

    public void setDailyCompletions(Map<String, Integer> dailyCompletions) {
        this.dailyCompletions = dailyCompletions;
    }

    public Map<String, Map<String, Double>> getUnitActionTotals() {
        return unitActionTotals;
    }

    public void setUnitActionTotals(Map<String, Map<String, Double>> unitActionTotals) {
        this.unitActionTotals = unitActionTotals;
    }

    public void updateStats(String category, String unit, String action, double value, String date, boolean increment) {
        int delta = increment ? 1 : -1;
        this.totalTasksCompleted = Math.max(0, this.totalTasksCompleted + delta);
        
        // 1. Update Daily Completions
        if (date != null && !date.isEmpty()) {
            Integer currentDay = dailyCompletions.getOrDefault(date, 0);
            dailyCompletions.put(date, Math.max(0, (currentDay != null ? currentDay : 0) + delta));
        }

        // 2. Update Unit Totals
        if (unit != null && !unit.isEmpty()) {
            String uKey = unit.toLowerCase().trim();
            Double currentUnit = unitTotals.getOrDefault(uKey, 0.0);
            unitTotals.put(uKey, increment ? (currentUnit != null ? currentUnit : 0.0) + value : Math.max(0, (currentUnit != null ? currentUnit : 0.0) - value));

            // 3. Update Granular Action Totals per Unit
            if (action != null && !action.isEmpty()) {
                String aKey = action.toLowerCase().trim();
                Map<String, Double> actionMap = unitActionTotals.computeIfAbsent(uKey, k -> new HashMap<>());
                Double currentAction = actionMap.getOrDefault(aKey, 0.0);
                actionMap.put(aKey, increment ? (currentAction != null ? currentAction : 0.0) + value : Math.max(0, (currentAction != null ? currentAction : 0.0) - value));
            }
        }

        // 4. Update Category Totals
        if (category != null) {
            String cKey = category.toLowerCase().trim();
            Double currentCat = categoryTotals.getOrDefault(cKey, 0.0);
            categoryTotals.put(cKey, increment ? (currentCat != null ? currentCat : 0.0) + value : Math.max(0, (currentCat != null ? currentCat : 0.0) - value));
        }
    }
}
