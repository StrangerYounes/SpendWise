package com.corner.takecontrol.ui.challenge;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

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

    public LiveData<Boolean> getActionComplete() {
        return actionComplete;
    }

    public void load(String challengeId) {
        this.challengeId = challengeId;
        removeListeners();

        challengeListener = challengeRepository.listenToChallenge(challengeId, new RepositoryCallback<Challenge>() {
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

        tasksListener = challengeRepository.listenToTasks(challengeId, new RepositoryCallback<List<ChallengeTask>>() {
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

        progressListener = challengeRepository.listenToProgress(challengeId, new RepositoryCallback<List<TaskProgress>>() {
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
            challengeRepository.completeChallenge(challengeId, new RepositoryCallback<Void>() {
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

        userRepository.getUserProfiles(current.getMemberIds(), new RepositoryCallback<Map<String, UserProfile>>() {
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
        challengeRepository.saveProgress(challengeId, progress, new RepositoryCallback<Void>() {
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

    public void shareChallenge() {
        Challenge current = challenge.getValue();
        if (current == null || challengeId == null) {
            return;
        }
        if (current.getShareCode() != null && !current.getShareCode().isEmpty()) {
            shareCode.setValue(current.getShareCode());
            return;
        }
        challengeRepository.generateShareCode(challengeId, new RepositoryCallback<String>() {
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
