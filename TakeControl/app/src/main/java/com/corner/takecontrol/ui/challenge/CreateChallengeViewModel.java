package com.corner.takecontrol.ui.challenge;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.corner.takecontrol.data.model.Challenge;
import com.corner.takecontrol.data.model.ChallengeTask;
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
    private final MutableLiveData<Challenge> challengeToEdit = new MutableLiveData<>();
    private final MutableLiveData<List<ChallengeTask>> tasksUpdated = new MutableLiveData<>();
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

    public LiveData<Challenge> getChallengeToEdit() {
        return challengeToEdit;
    }

    public LiveData<List<ChallengeTask>> getTasksUpdated() {
        return tasksUpdated;
    }

    public List<ChallengeTask> getPendingTasks() {
        return pendingTasks;
    }

    public void addPendingTask(ChallengeTask task) {
        task.setOrderIndex(pendingTasks.size());
        pendingTasks.add(task);
        tasksUpdated.setValue(new ArrayList<>(pendingTasks));
    }

    public void removePendingTask(int index) {
        if (index >= 0 && index < pendingTasks.size()) {
            ChallengeTask removed = pendingTasks.remove(index);
            if (currentChallengeId != null && removed.getId() != null) {
                challengeRepository.deleteTask(currentChallengeId, removed.getId(), new RepositoryCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {}
                    @Override
                    public void onError(String message) {
                        error.setValue(message);
                    }
                });
            }
            for (int i = 0; i < pendingTasks.size(); i++) {
                pendingTasks.get(i).setOrderIndex(i);
            }
            tasksUpdated.setValue(new ArrayList<>(pendingTasks));
        }
    }

    public void loadChallenge(String id) {
        currentChallengeId = id;
        loading.setValue(true);
        challengeRepository.getChallenge(id, new RepositoryCallback<Challenge>() {
            @Override
            public void onSuccess(Challenge result) {
                challengeToEdit.setValue(result);
                loadTasks(id);
            }

            @Override
            public void onError(String message) {
                loading.setValue(false);
                error.setValue(message);
            }
        });
    }

    private void loadTasks(String id) {
        challengeRepository.getTasks(id, new RepositoryCallback<List<ChallengeTask>>() {
            @Override
            public void onSuccess(List<ChallengeTask> result) {
                loading.setValue(false);
                pendingTasks.clear();
                pendingTasks.addAll(result);
                tasksUpdated.setValue(new ArrayList<>(pendingTasks));
            }

            @Override
            public void onError(String message) {
                loading.setValue(false);
                error.setValue(message);
            }
        });
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

        if (currentChallengeId != null) {
            updateChallenge(title, description, durationDays);
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

    private void updateChallenge(String title, String description, int durationDays) {
        loading.setValue(true);
        Challenge challenge = challengeToEdit.getValue();
        if (challenge == null) {
            // If we don't have the object, we just update the ID part (partial update not supported by repo yet)
            // But loadChallenge should have populated this.
            loading.setValue(false);
            return;
        }
        challenge.setTitle(title.trim());
        challenge.setDescription(description != null ? description.trim() : "");
        challenge.setDurationDays(durationDays);

        challengeRepository.updateChallenge(currentChallengeId, challenge, new RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
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
        
        // If task already has an ID, we don't need to re-add it (optional: update it)
        if (task.getId() != null) {
            saveTasksSequentially(index + 1);
            return;
        }

        challengeRepository.addTask(currentChallengeId, task, new RepositoryCallback<String>() {
            @Override
            public void onSuccess(String result) {
                task.setId(result);
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
        challengeToEdit.setValue(null);
    }
}
