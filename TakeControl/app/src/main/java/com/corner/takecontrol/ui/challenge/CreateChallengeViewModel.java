package com.corner.takecontrol.ui.challenge;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.corner.takecontrol.data.model.Challenge;
import com.corner.takecontrol.data.model.ChallengeTask;
import com.corner.takecontrol.data.model.ChallengeTemplate;
import com.corner.takecontrol.data.repository.ChallengeRepository;
import com.corner.takecontrol.data.repository.RepositoryCallback;
import com.corner.takecontrol.data.repository.TemplateRepository;
import com.corner.takecontrol.util.SecurityUtil;
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

    public void addPendingTask(ChallengeTask task) {
        task.setOrderIndex(pendingTasks.size());
        pendingTasks.add(task);
        tasksUpdated.setValue(new ArrayList<>(pendingTasks));
    }

    public void updatePendingTask(int index, ChallengeTask task) {
        if (index >= 0 && index < pendingTasks.size()) {
            ChallengeTask oldTask = pendingTasks.get(index);
            task.setOrderIndex(oldTask.getOrderIndex());
            task.setId(oldTask.getId());
            pendingTasks.set(index, task);
            tasksUpdated.setValue(new ArrayList<>(pendingTasks));
        }
    }

    public void moveTask(int fromPosition, int toPosition) {
        if (fromPosition < 0 || fromPosition >= pendingTasks.size() ||
                toPosition < 0 || toPosition >= pendingTasks.size()) return;

        ChallengeTask task = pendingTasks.remove(fromPosition);
        pendingTasks.add(toPosition, task);

        for (int i = 0; i < pendingTasks.size(); i++) {
            pendingTasks.get(i).setOrderIndex(i);
        }
        tasksUpdated.setValue(new ArrayList<>(pendingTasks));
    }

    public void removePendingTask(int index) {
        if (index >= 0 && index < pendingTasks.size()) {
            ChallengeTask removed = pendingTasks.remove(index);
            if (currentChallengeId != null && removed.getId() != null) {
                challengeRepository.deleteTask(currentChallengeId, removed.getId(), new RepositoryCallback<>() {
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
        challengeRepository.getChallenge(id, new RepositoryCallback<>() {
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

    public void loadTemplate(String templateId) {
        TemplateRepository repository = new TemplateRepository();
        List<ChallengeTemplate> templates = repository.getTemplates();
        for (ChallengeTemplate template : templates) {
            if (template.getId().equals(templateId)) {
                Challenge c = new Challenge();
                c.setTitle(template.getTitle());
                c.setDescription(template.getDescription());
                c.setDurationDays(template.getDurationDays());
                challengeToEdit.setValue(c);

                pendingTasks.clear();
                pendingTasks.addAll(template.getTasks());
                tasksUpdated.setValue(new ArrayList<>(pendingTasks));
                return;
            }
        }
    }

    private void loadTasks(String id) {
        challengeRepository.getTasks(id, new RepositoryCallback<>() {
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

    public void createChallenge(String title, String description, int durationDays, int maxSkips, boolean isPublic, String password) {
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

        String encryptedPassword = null;
        if (!isPublic && password != null && !password.isEmpty()) {
            try {
                encryptedPassword = SecurityUtil.encrypt(password);
            } catch (Exception e) {
                error.setValue("Encryption failed: " + e.getMessage());
                return;
            }
        }

        if (currentChallengeId != null) {
            updateChallenge(title, description, durationDays, maxSkips, isPublic, encryptedPassword);
            return;
        }

        loading.setValue(true);
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        challengeRepository.createChallenge(userId, title.trim(), description != null ? description.trim() : "",
                durationDays, maxSkips, isPublic, encryptedPassword, new RepositoryCallback<>() {
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

    private void updateChallenge(String title, String description, int durationDays, int maxSkips, boolean isPublic, String encryptedPassword) {
        loading.setValue(true);
        Challenge challenge = challengeToEdit.getValue();
        if (challenge == null) {
            loading.setValue(false);
            return;
        }
        challenge.setTitle(title.trim());
        challenge.setDescription(description != null ? description.trim() : "");
        challenge.setDurationDays(durationDays);
        challenge.setMaxSkips(maxSkips);
        challenge.setPublic(isPublic);
        challenge.setEncryptedPassword(encryptedPassword);

        challengeRepository.updateChallenge(currentChallengeId, challenge, new RepositoryCallback<>() {
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
        
        if (task.getId() != null) {
            challengeRepository.updateTask(currentChallengeId, task, new RepositoryCallback<>() {
                @Override
                public void onSuccess(Void result) {
                    saveTasksSequentially(index + 1);
                }

                @Override
                public void onError(String message) {
                    loading.setValue(false);
                    error.setValue(message);
                }
            });
            return;
        }

        challengeRepository.addTask(currentChallengeId, task, new RepositoryCallback<>() {
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
