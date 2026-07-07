package com.corner.takecontrol.data.repository;

import com.corner.takecontrol.data.model.ChallengeTask;
import com.corner.takecontrol.data.model.ChallengeTemplate;

import java.util.ArrayList;
import java.util.List;

public class TemplateRepository {

    public List<ChallengeTemplate> getTemplates() {
        List<ChallengeTemplate> templates = new ArrayList<>();

        // 7-Day Digital Detox
        List<ChallengeTask> detoxTasks = new ArrayList<>();
        detoxTasks.add(new ChallengeTask("No Social Media after 8 PM", "daily", "checkmark", 1, "", 0));
        detoxTasks.add(new ChallengeTask("Read for 20 mins", "daily", "duration", 20, "min", 1));
        templates.add(new ChallengeTemplate("detox", "7-Day Digital Detox", "Unplug and reclaim your focus.", 7, detoxTasks));

        // 30-Day Fitness
        List<ChallengeTask> fitnessTasks = new ArrayList<>();
        fitnessTasks.add(new ChallengeTask("Morning Stretch", "daily", "checkmark", 1, "", 0));
        fitnessTasks.add(new ChallengeTask("Drink 2L Water", "daily", "numeric", 2, "L", 1));
        fitnessTasks.add(new ChallengeTask("Daily Walk", "daily", "duration", 30, "min", 2));
        templates.add(new ChallengeTemplate("fitness", "30-Day Fitness", "Build a healthier lifestyle step by step.", 30, fitnessTasks));

        // Healthy Habits
        List<ChallengeTask> habitsTasks = new ArrayList<>();
        habitsTasks.add(new ChallengeTask("Meditate", "daily", "duration", 10, "min", 0));
        habitsTasks.add(new ChallengeTask("Sleep by 11 PM", "daily", "checkmark", 1, "", 1));
        templates.add(new ChallengeTemplate("habits", "Healthy Habits", "Improve your daily routine.", 21, habitsTasks));

        return templates;
    }
}
