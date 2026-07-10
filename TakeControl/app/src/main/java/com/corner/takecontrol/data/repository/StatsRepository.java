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
                    
                    String category = TaskCategorizer.categorize(task.getTitle());
                    stats.updateStats(category, task.getUnit(), value, increment);

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
                    callback.onSuccess(stats);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public ListenerRegistration listenToUserStats(String userId, RepositoryCallback<UserStats> callback) {
        return firestore.collection(COLLECTION_STATS).document(userId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        callback.onError(error.getMessage());
                        return;
                    }
                    if (snapshot == null || !snapshot.exists()) {
                        callback.onSuccess(null);
                        return;
                    }
                    UserStats stats = snapshot.toObject(UserStats.class);
                    if (stats != null) {
                        stats.setUserId(snapshot.getId());
                    }
                    callback.onSuccess(stats);
                });
    }
}
