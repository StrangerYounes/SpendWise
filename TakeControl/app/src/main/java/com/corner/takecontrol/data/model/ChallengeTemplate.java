package com.corner.takecontrol.data.model;

import java.util.List;

public class ChallengeTemplate {
    private String id;
    private String title;
    private String description;
    private int durationDays;
    private List<ChallengeTask> tasks;

    public ChallengeTemplate() {}

    public ChallengeTemplate(String id, String title, String description, int durationDays, List<ChallengeTask> tasks) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.durationDays = durationDays;
        this.tasks = tasks;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public int getDurationDays() {
        return durationDays;
    }

    public List<ChallengeTask> getTasks() {
        return tasks;
    }
}
