package com.corner.takecontrol.ui.challenge;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.corner.takecontrol.R;
import com.corner.takecontrol.TakeControlApplication;
import com.corner.takecontrol.data.model.AppNotification;
import com.corner.takecontrol.data.model.Challenge;
import com.corner.takecontrol.data.model.ChallengeStatus;
import com.corner.takecontrol.data.model.ChallengeTask;
import com.corner.takecontrol.data.model.LeaderboardEntry;
import com.corner.takecontrol.data.model.TaskProgress;
import com.corner.takecontrol.data.model.UserProfile;
import com.corner.takecontrol.data.repository.ChallengeRepository;
import com.corner.takecontrol.data.repository.RepositoryCallback;
import com.corner.takecontrol.data.repository.UserRepository;
import com.corner.takecontrol.util.ProgressCalculator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;
import java.util.Map;

public class ChallengeDetailViewModel extends ViewModel {

    private final ChallengeRepository challengeRepository;
    private final UserRepository userRepository;
    private final MutableLiveData<Challenge> challenge = new MutableLiveData<>();
    private final MutableLiveData<List<ChallengeTask>> tasks = new MutableLiveData<>();
    private final MutableLiveData<List<TaskProgress>> progressList = new MutableLiveData<>();
    private final MutableLiveData<Integer> completionPercent = new MutableLiveData<>(0);
    private final MutableLiveData<List<LeaderboardEntry>> leaderboard = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<String> shareCode = new MutableLiveData<>();
    private final MutableLiveData<Boolean> actionComplete = new MutableLiveData<>();
    private final MutableLiveData<Boolean> challengeDeleted = new MutableLiveData<>();

    private ListenerRegistration challengeListener;
    private ListenerRegistration tasksListener;
    private ListenerRegistration progressListener;
    private String challengeId;

    public ChallengeDetailViewModel() {
        challengeRepository = new ChallengeRepository();
        userRepository = new UserRepository();
    }

    public LiveData<Challenge> getChallenge() {
        return challenge;
    }

    public LiveData<List<ChallengeTask>> getTasks() {
        return tasks;
    }

    public LiveData<List<TaskProgress>> getProgressList() {
        return progressList;
    }

    public LiveData<Integer> getCompletionPercent() {
        return completionPercent;
    }

    public LiveData<List<LeaderboardEntry>> getLeaderboard() {
        return leaderboard;
    }

    public LiveData<String> getError() {
        return error;
    }

    public LiveData<String> getShareCode() {
        return shareCode;
    }

    public LiveData<Boolean> getChallengeDeleted() {
        return challengeDeleted;
    }

    public void load(String challengeId) {
        this.challengeId = challengeId;
        removeListeners();

        challengeListener = challengeRepository.listenToChallenge(challengeId, new RepositoryCallback<>() {
            @Override
            public void onSuccess(Challenge result) {
                challenge.setValue(result);
                updateCompletion();
                maybeRefreshLeaderboard();
            }

            @Override
            public void onError(String message) {
                error.setValue(message);
            }
        });

        tasksListener = challengeRepository.listenToTasks(challengeId, new RepositoryCallback<>() {
            @Override
            public void onSuccess(List<ChallengeTask> result) {
                tasks.setValue(result);
                updateCompletion();
                maybeRefreshLeaderboard();
            }

            @Override
            public void onError(String message) {
                error.setValue(message);
            }
        });

        progressListener = challengeRepository.listenToProgress(challengeId, new RepositoryCallback<>() {
            @Override
            public void onSuccess(List<TaskProgress> result) {
                progressList.setValue(result);
                updateCompletion();
                maybeRefreshLeaderboard();
            }

            @Override
            public void onError(String message) {
                error.setValue(message);
            }
        });
    }

    private void updateCompletion() {
        Challenge current = challenge.getValue();
        List<ChallengeTask> currentTasks = tasks.getValue();
        List<TaskProgress> currentProgress = progressList.getValue();
        if (current == null || currentTasks == null || FirebaseAuth.getInstance().getCurrentUser() == null) {
            return;
        }

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        int percent = ProgressCalculator.calculateCompletionPercent(current, currentTasks, currentProgress, userId);
        completionPercent.setValue(percent);

        if (current.getStatusEnum() == ChallengeStatus.ACTIVE
                && ProgressCalculator.shouldMarkChallengeCompleted(current, currentTasks, currentProgress, userId)) {
            challengeRepository.completeChallenge(challengeId, new RepositoryCallback<>() {
                @Override
                public void onSuccess(Void result) {
                    actionComplete.setValue(true);
                }

                @Override
                public void onError(String message) {
                    error.setValue(message);
                }
            });
        }
    }

    private void maybeRefreshLeaderboard() {
        Challenge current = challenge.getValue();
        List<ChallengeTask> currentTasks = tasks.getValue();
        List<TaskProgress> currentProgress = progressList.getValue();
        if (current == null || currentTasks == null || currentProgress == null) {
            return;
        }
        if (current.getMemberCount() < 2) {
            leaderboard.setValue(null);
            return;
        }

        userRepository.getUserProfiles(current.getMemberIds(), new RepositoryCallback<>() {
            @Override
            public void onSuccess(Map<String, UserProfile> profiles) {
                leaderboard.setValue(ProgressCalculator.buildLeaderboard(current, currentTasks, currentProgress, profiles));
            }

            @Override
            public void onError(String message) {
                error.setValue(message);
            }
        });
    }

    public void startChallenge() {
        Challenge current = challenge.getValue();
        if (current == null || challengeId == null) {
            return;
        }
        if (tasks.getValue() == null || tasks.getValue().isEmpty()) {
            error.setValue("Add at least one task before starting");
            return;
        }
        challengeRepository.startChallenge(challengeId, current, new RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                actionComplete.setValue(true);
            }

            @Override
            public void onError(String message) {
                error.setValue(message);
            }
        });
    }

    public void saveProgress(TaskProgress progress) {
        if (challengeId == null) {
            return;
        }

        // Find existing progress to check for completion state change
        TaskProgress oldProgress = null;
        List<TaskProgress> currentList = progressList.getValue();
        if (currentList != null) {
            for (TaskProgress p : currentList) {
                if (p.getTaskId().equals(progress.getTaskId()) && 
                    p.getUserId().equals(progress.getUserId()) && 
                    p.getPeriodKey().equals(progress.getPeriodKey())) {
                    oldProgress = p;
                    break;
                }
            }
        }

        final boolean wasCompleted = oldProgress != null && oldProgress.isCompleted();
        final boolean isNowCompleted = progress.isCompleted();
        final boolean wasLate = oldProgress != null && "LATE".equals(oldProgress.getStatus());
        final boolean isNowLate = "LATE".equals(progress.getStatus());

        challengeRepository.saveProgress(challengeId, progress, new RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                actionComplete.setValue(true);
                if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
                String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                ChallengeTask task = findTaskById(progress.getTaskId());
                if (task == null) return;

                if (!wasCompleted && isNowCompleted) {
                    userRepository.rewardTaskCompletion(userId, task, isNowLate, new RepositoryCallback<>() {
                        @Override
                        public void onSuccess(Void result) {}
                        @Override
                        public void onError(String message) {}
                    });
                } else if (wasCompleted && !isNowCompleted) {
                    userRepository.deductTaskCompletion(userId, task, wasLate, new RepositoryCallback<>() {
                        @Override
                        public void onSuccess(Void result) {}
                        @Override
                        public void onError(String message) {}
                    });
                }
            }

            @Override
            public void onError(String message) {
                error.setValue(message);
            }
        });
    }

    private ChallengeTask findTaskById(String taskId) {
        List<ChallengeTask> currentTasks = tasks.getValue();
        if (currentTasks != null) {
            for (ChallengeTask task : currentTasks) {
                if (task.getId().equals(taskId)) {
                    return task;
                }
            }
        }
        return null;
    }

    public void shareChallenge() {
        Challenge current = challenge.getValue();
        if (current == null || challengeId == null) {
            return;
        }
        if (current.getShareCode() != null && !current.getShareCode().isEmpty()) {
            shareCode.setValue(current.getShareCode());
            return;
        }
        challengeRepository.generateShareCode(challengeId, new RepositoryCallback<>() {
            @Override
            public void onSuccess(String result) {
                shareCode.setValue(result);
            }

            @Override
            public void onError(String message) {
                error.setValue(message);
            }
        });
    }

    public void nudgeMember(String toUserId, String toUserName) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String fromUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        userRepository.getUserProfile(fromUserId, new RepositoryCallback<>() {
            @Override
            public void onSuccess(UserProfile fromProfile) {
                String fromName = fromProfile != null ? fromProfile.getDisplayName() : "Someone";
                String message = String.format(java.util.Locale.US, "%s nudged you!", fromName);
                AppNotification notification = new AppNotification(toUserId, fromUserId, fromName, "NUDGE", message, challengeId);

                userRepository.sendNotification(toUserId, notification, new RepositoryCallback<>() {
                    @Override
                    public void onSuccess(Void result) {
                    }
                    @Override
                    public void onError(String message) {
                        error.setValue(message);
                    }
                });
            }
            @Override
            public void onError(String message) {
                error.setValue(message);
            }
        });
    }

    public void useSkipDay() {
        Challenge current = challenge.getValue();
        if (current == null || challengeId == null || FirebaseAuth.getInstance().getCurrentUser() == null) {
            return;
        }
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        
        int skipsUsed = current.getMemberSkips() != null ? current.getMemberSkips().getOrDefault(userId, 0) : 0;
        if (skipsUsed >= current.getMaxSkips()) {
            error.setValue(TakeControlApplication.getAppContext().getString(R.string.no_skips));
            return;
        }

        // Mark ALL tasks for today as skipped (completed with a special status)
        List<ChallengeTask> currentTasks = tasks.getValue();
        if (currentTasks == null) return;

        for (ChallengeTask task : currentTasks) {
            String periodKey = com.corner.takecontrol.util.PeriodKeyUtil.getCurrentPeriodKey(task.getFrequencyEnum());
            TaskProgress p = new TaskProgress(userId, task.getId(), periodKey, task.getTargetValue(), true, "SKIPPED");
            challengeRepository.saveProgress(challengeId, p, new RepositoryCallback<Void>() {
                @Override
                public void onSuccess(Void result) {}
                @Override
                public void onError(String message) {}
            });
        }

        // Increment skip count
        java.util.Map<String, Integer> skipsMap = current.getMemberSkips();
        if (skipsMap == null) skipsMap = new java.util.HashMap<>();
        skipsMap.put(userId, skipsUsed + 1);
        current.setMemberSkips(skipsMap);

        challengeRepository.updateChallenge(challengeId, current, new RepositoryCallback<>() {
            @Override
            public void onSuccess(Void result) {
                actionComplete.setValue(true);
            }
            @Override
            public void onError(String message) {
                error.setValue(message);
            }
        });
    }

    public void deleteChallenge() {
        if (challengeId == null) return;
        challengeRepository.deleteChallenge(challengeId, new RepositoryCallback<>() {
            @Override
            public void onSuccess(Void result) {
                challengeDeleted.setValue(true);
            }

            @Override
            public void onError(String message) {
                error.setValue(message);
            }
        });
    }

    public void leaveChallenge() {
        if (challengeId == null || FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        challengeRepository.leaveChallenge(challengeId, userId, new RepositoryCallback<>() {
            @Override
            public void onSuccess(Void result) {
                challengeDeleted.setValue(true);
            }

            @Override
            public void onError(String message) {
                error.setValue(message);
            }
        });
    }

    public void kickMember(String memberUid) {
        if (challengeId == null) return;
        challengeRepository.kickMember(challengeId, memberUid, new RepositoryCallback<>() {
            @Override
            public void onSuccess(Void result) {
                actionComplete.setValue(true);
            }

            @Override
            public void onError(String message) {
                error.setValue(message);
            }
        });
    }

    public void archiveChallenge() {
        if (challengeId == null || FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        challengeRepository.archiveChallenge(challengeId, userId, new RepositoryCallback<>() {
            @Override
            public void onSuccess(Void result) {
                challengeDeleted.setValue(true);
            }

            @Override
            public void onError(String message) {
                error.setValue(message);
            }
        });
    }

    private void removeListeners() {
        if (challengeListener != null) {
            challengeListener.remove();
            challengeListener = null;
        }
        if (tasksListener != null) {
            tasksListener.remove();
            tasksListener = null;
        }
        if (progressListener != null) {
            progressListener.remove();
            progressListener = null;
        }
    }

    @Override
    protected void onCleared() {
        removeListeners();
        super.onCleared();
    }
}
