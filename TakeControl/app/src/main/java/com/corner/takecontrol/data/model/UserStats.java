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

    public void updateStats(String category, String unit, double value, boolean increment) {
        int delta = increment ? 1 : -1;
        this.totalTasksCompleted = Math.max(0, this.totalTasksCompleted + delta);
        
        if (unit != null && !unit.isEmpty()) {
            String uKey = unit.toLowerCase().trim();
            double currentUnit = unitTotals.getOrDefault(uKey, 0.0);
            unitTotals.put(uKey, increment ? currentUnit + value : Math.max(0, currentUnit - value));
        }

        if (category != null) {
            String cKey = category.toLowerCase().trim();
            double currentCat = categoryTotals.getOrDefault(cKey, 0.0);
            categoryTotals.put(cKey, increment ? currentCat + value : Math.max(0, currentCat - value));
        }
    }
}
