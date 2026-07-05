package com.corner.takecontrol.ui.challenge;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.corner.takecontrol.data.model.Challenge;
import com.corner.takecontrol.data.model.ChallengeTask;
import com.corner.takecontrol.data.model.TaskType;
import com.corner.takecontrol.data.repository.ChallengeRepository;
import com.corner.takecontrol.data.repository.RepositoryCallback;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class CreateChallengeViewModel extends ViewModel {

    private final ChallengeRepository challengeRepository;
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<String> challengeCreated = new MutableLiveData<>();
    private final List<ChallengeTask> pendingTasks = new ArrayList<>();
    private String currentChallengeId;

    public CreateChallengeViewModel() {
        challengeRepository = new ChallengeRepository();
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<String> getError() {
        return error;
    }

    public LiveData<String> getChallengeCreated() {
        return challengeCreated;
    }

    public List<ChallengeTask> getPendingTasks() {
        return pendingTasks;
    }

    public void addPendingTask(ChallengeTask task) {
        task.setOrderIndex(pendingTasks.size());
        pendingTasks.add(task);
    }

    public void removePendingTask(int index) {
        if (index >= 0 && index < pendingTasks.size()) {
            pendingTasks.remove(index);
            for (int i = 0; i < pendingTasks.size(); i++) {
                pendingTasks.get(i).setOrderIndex(i);
            }
        }
    }

    public void createChallenge(String title, String description, int durationDays) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            error.setValue("You must be signed in");
            return;
        }
        if (title == null || title.trim().isEmpty()) {
            error.setValue("Title is required");
            return;
        }
        if (pendingTasks.isEmpty()) {
            error.setValue("Add at least one task");
            return;
        }

        loading.setValue(true);
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        challengeRepository.createChallenge(userId, title.trim(), description != null ? description.trim() : "",
                durationDays, new RepositoryCallback<String>() {
                    @Override
                    public void onSuccess(String challengeId) {
                        currentChallengeId = challengeId;
                        saveTasksSequentially(0);
                    }

                    @Override
                    public void onError(String message) {
                        loading.setValue(false);
                        error.setValue(message);
                    }
                });
    }

    private void saveTasksSequentially(int index) {
        if (index >= pendingTasks.size()) {
            loading.setValue(false);
            challengeCreated.setValue(currentChallengeId);
            return;
        }
        ChallengeTask task = pendingTasks.get(index);
        challengeRepository.addTask(currentChallengeId, task, new RepositoryCallback<String>() {
            @Override
            public void onSuccess(String result) {
                saveTasksSequentially(index + 1);
            }

            @Override
            public void onError(String message) {
                loading.setValue(false);
                error.setValue(message);
            }
        });
    }

    public void clear() {
        pendingTasks.clear();
        currentChallengeId = null;
    }
}
