package com.corner.takecontrol;

public class FeatureFlags {
    public static final boolean STATISTICS_UI_ENABLED = true;
    
    // Level thresholds for unlocking statistics sections
    public static final int LVL_THRESHOLD_VOLUME = 3;      // Production target: 10
    public static final int LVL_THRESHOLD_CATEGORIES = 5;  // Production target: 15
    public static final int LVL_THRESHOLD_GRAPHS = 1;      // Weekly Graph
    public static final int LVL_THRESHOLD_HEATMAP = 2;     // Productivity Heatmap
}
