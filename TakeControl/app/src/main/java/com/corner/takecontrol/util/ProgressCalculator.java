package com.corner.takecontrol.util;

import com.corner.takecontrol.data.model.Challenge;
import com.corner.takecontrol.data.model.ChallengeStatus;
import com.corner.takecontrol.data.model.ChallengeTask;
import com.corner.takecontrol.data.model.LeaderboardEntry;
import com.corner.takecontrol.data.model.TaskFrequency;
import com.corner.takecontrol.data.model.TaskProgress;
import com.corner.takecontrol.data.model.TaskType;
import com.corner.takecontrol.data.model.UserProfile;
import com.google.firebase.Timestamp;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ProgressCalculator {

    private ProgressCalculator() {
    }

    public static int calculateCompletionPercent(Challenge challenge,
                                                 List<ChallengeTask> tasks,
                                                 List<TaskProgress> progressList,
                                                 String userId) {
        if (challenge == null || tasks == null || tasks.isEmpty()) {
            return 0;
        }

        LocalDate start = toLocalDate(challenge.getStartDate());
        LocalDate end = getEffectiveEndDate(challenge);
        if (start == null || end == null) {
            return 0;
        }

        int expected = 0;
        int completed = 0;
        Map<String, TaskProgress> progressMap = mapProgressForUser(progressList, userId);

        for (ChallengeTask task : tasks) {
            TaskFrequency frequency = task.getFrequencyEnum();
            List<String> periodKeys = PeriodKeyUtil.getPeriodKeysBetween(frequency, start, end);
            expected += periodKeys.size();
            for (String periodKey : periodKeys) {
                TaskProgress progress = progressMap.get(task.getId() + "_" + periodKey);
                if (isPeriodComplete(task, progress)) {
                    completed++;
                }
            }
        }

        if (expected == 0) {
            return 0;
        }
        return Math.round((completed * 100f) / expected);
    }

    public static boolean isPeriodComplete(ChallengeTask task, TaskProgress progress) {
        if (progress == null) {
            return false;
        }
        TaskType type = task.getTaskTypeEnum();
        if (type == TaskType.CHECKMARK) {
            return progress.isCompleted();
        }
        return progress.getValue() >= task.getTargetValue();
    }

    public static boolean isTaskCompleteForCurrentPeriod(ChallengeTask task, TaskProgress progress) {
        return isPeriodComplete(task, progress);
    }

    public static boolean shouldMarkChallengeCompleted(Challenge challenge,
                                                       List<ChallengeTask> tasks,
                                                       List<TaskProgress> progressList,
                                                       String userId) {
        if (challenge.getStatusEnum() != ChallengeStatus.ACTIVE) {
            return false;
        }
        return calculateCompletionPercent(challenge, tasks, progressList, userId) >= 100;
    }

    public static List<LeaderboardEntry> buildLeaderboard(Challenge challenge,
                                                          List<ChallengeTask> tasks,
                                                          List<TaskProgress> allProgress,
                                                          Map<String, UserProfile> profiles) {
        if (challenge == null || challenge.getMemberIds() == null) {
            return Collections.emptyList();
        }

        List<LeaderboardEntry> entries = new ArrayList<>();
        for (String memberId : challenge.getMemberIds()) {
            int percent = calculateCompletionPercent(challenge, tasks, allProgress, memberId);
            UserProfile profile = profiles.get(memberId);
            String name = profile != null && profile.getDisplayName() != null
                    ? profile.getDisplayName()
                    : "Member";
            entries.add(new LeaderboardEntry(memberId, name, percent));
        }

        entries.sort(Comparator.comparingInt(LeaderboardEntry::getCompletionPercent).reversed());
        return entries;
    }

    private static Map<String, TaskProgress> mapProgressForUser(List<TaskProgress> progressList, String userId) {
        Map<String, TaskProgress> map = new HashMap<>();
        if (progressList == null) {
            return map;
        }
        for (TaskProgress progress : progressList) {
            if (userId.equals(progress.getUserId())) {
                map.put(progress.getTaskId() + "_" + progress.getPeriodKey(), progress);
            }
        }
        return map;
    }

    public static LocalDate toLocalDate(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        return timestamp.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    public static LocalDate getEffectiveEndDate(Challenge challenge) {
        LocalDate end = toLocalDate(challenge.getEndDate());
        LocalDate today = LocalDate.now();
        if (end == null) {
            return today;
        }
        return end.isBefore(today) ? end : today;
    }

    public static Set<String> getUniqueUserIds(List<TaskProgress> progressList) {
        Set<String> ids = new HashSet<>();
        if (progressList != null) {
            for (TaskProgress progress : progressList) {
                ids.add(progress.getUserId());
            }
        }
        return ids;
    }
}
