package com.corner.takecontrol.data.repository;

import com.corner.takecontrol.data.model.ChallengeTask;
import com.corner.takecontrol.data.model.UserStats;
import com.corner.takecontrol.util.TaskCategorizer;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class StatsRepository {

    private static final String COLLECTION_STATS = "user_stats";
    private final FirebaseFirestore firestore;

    public StatsRepository() {
        firestore = FirebaseFirestore.getInstance();
    }

    public void updateStats(String userId, ChallengeTask task, double value, boolean increment, RepositoryCallback<Void> callback) {
        firestore.collection(COLLECTION_STATS).document(userId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    UserStats stats = snapshot.toObject(UserStats.class);
                    if (stats == null) {
                        stats = new UserStats();
                        stats.setUserId(userId);
                    }
                    
                    String category = task.getManualCategory();
                    if (category == null || category.isEmpty()) {
                        category = TaskCategorizer.categorize(task.getTitle());
                    }
                    
                    String action = task.getManualAction();
                    if (action == null || action.isEmpty()) {
                        action = TaskCategorizer.extractAction(task.getTitle());
                    }
                    
                    java.util.Calendar cal = java.util.Calendar.getInstance();
                    String today = String.format(java.util.Locale.US, "%04d-%02d-%02d",
                            cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH) + 1, cal.get(java.util.Calendar.DAY_OF_MONTH));

                    stats.updateStats(category, task.getUnit(), action, value, today, increment);

                    firestore.collection(COLLECTION_STATS).document(userId)
                            .set(stats)
                            .addOnSuccessListener(unused -> {
                                if (callback != null) callback.onSuccess(null);
                            })
                            .addOnFailureListener(e -> {
                                if (callback != null) callback.onError(e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    public void getUserStats(String userId, RepositoryCallback<UserStats> callback) {
        firestore.collection(COLLECTION_STATS).document(userId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    UserStats stats = snapshot.toObject(UserStats.class);
                    if (stats != null) {
                        stats.setUserId(snapshot.getId());
                    }
                    if (callback != null) callback.onSuccess(stats);
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    public ListenerRegistration listenToUserStats(String userId, RepositoryCallback<UserStats> callback) {
        return firestore.collection(COLLECTION_STATS).document(userId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        if (callback != null) callback.onError(error.getMessage());
                        return;
                    }
                    if (snapshot == null || !snapshot.exists()) {
                        if (callback != null) callback.onSuccess(null);
                        return;
                    }
                    UserStats stats = snapshot.toObject(UserStats.class);
                    if (stats != null) {
                        stats.setUserId(snapshot.getId());
                    }
                    if (callback != null) callback.onSuccess(stats);
                });
    }
}
