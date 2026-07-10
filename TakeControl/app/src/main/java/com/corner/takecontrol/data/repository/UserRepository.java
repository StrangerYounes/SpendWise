package com.corner.takecontrol.data.repository;

import com.corner.takecontrol.data.model.AppNotification;
import com.corner.takecontrol.data.model.ChallengeTask;
import com.corner.takecontrol.data.model.UserProfile;
import com.corner.takecontrol.util.XpUtil;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class UserRepository {

    private final FirebaseFirestore firestore;

    public UserRepository() {
        firestore = FirebaseFirestore.getInstance();
    }

    public void getUserProfile(String userId, RepositoryCallback<UserProfile> callback) {
        firestore.collection("users").document(userId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    UserProfile profile = snapshot.toObject(UserProfile.class);
                    if (profile != null) {
                        profile.setId(snapshot.getId());
                    }
                    callback.onSuccess(profile);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public ListenerRegistration listenToUserProfile(String userId, RepositoryCallback<UserProfile> callback) {
        return firestore.collection("users").document(userId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        callback.onError(error.getMessage());
                        return;
                    }
                    if (snapshot == null || !snapshot.exists()) {
                        callback.onSuccess(null);
                        return;
                    }
                    UserProfile profile = snapshot.toObject(UserProfile.class);
                    if (profile != null) {
                        profile.setId(snapshot.getId());
                    }
                    callback.onSuccess(profile);
                });
    }

    public void updateUserProfile(String userId, UserProfile profile, RepositoryCallback<Void> callback) {
        firestore.collection("users").document(userId)
                .set(profile)
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // TODO: Implement uploadProfileImage(String userId, byte[] encryptedData, ...) using 
    // com.google.firebase.storage.FirebaseStorage if project is upgraded to a paid tier.

    public void rewardTaskCompletion(String userId, ChallengeTask task, boolean isLate, RepositoryCallback<Void> callback) {
        getUserProfile(userId, new RepositoryCallback<>() {
            @Override
            public void onSuccess(UserProfile profile) {
                if (profile == null) {
                    callback.onError("User profile not found");
                    return;
                }

                Calendar cal = Calendar.getInstance();
                String today = String.format(Locale.US, "%04d-%02d-%02d",
                        cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));
                cal.add(Calendar.DAY_OF_YEAR, -1);
                String yesterday = String.format(Locale.US, "%04d-%02d-%02d",
                        cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));
                cal.add(Calendar.DAY_OF_YEAR, 1); // Reset to today

                String lastDate = profile.getLastCompletionDate();

                // 1. Update Streak (if first task today)
                if (!Objects.equals(today, lastDate)) {
                    int streak = profile.getCurrentStreak();
                    if (Objects.equals(yesterday, lastDate)) {
                        streak++;
                    } else {
                        streak = 1;
                    }
                    profile.setCurrentStreak(streak);
                    profile.setLastCompletionDate(today);
                    if (streak > profile.getLongestStreak()) {
                        profile.setLongestStreak(streak);
                    }
                }

                // 2. Reward XP
                long xpReward = XpUtil.getXpReward(task.getTaskType(), isLate);
                profile.setXp(profile.getXp() + xpReward);
                profile.setLevel(XpUtil.calculateLevel(profile.getXp()));

                // 3. Unlock Progression Rewards
                com.corner.takecontrol.util.ProgressionUtil.checkAndUnlockRewards(profile);

                // 4. Check Achievements
                List<String> achievements = profile.getAchievements();
                if (achievements == null) achievements = new ArrayList<>();

                // Achievement: Early Bird (completed on time)
                if (!isLate && !achievements.contains("EARLY_BIRD")) {
                    achievements.add("EARLY_BIRD");
                }

                // Achievement: Weekend Warrior (completed on Sat or Sun)
                int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
                if ((dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) && !achievements.contains("WEEKEND_WARRIOR")) {
                    achievements.add("WEEKEND_WARRIOR");
                }

                profile.setAchievements(achievements);

                firestore.collection("users").document(userId)
                        .set(profile)
                        .addOnSuccessListener(unused -> callback.onSuccess(null))
                        .addOnFailureListener(e -> callback.onError(e.getMessage()));
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    public void deductTaskCompletion(String userId, ChallengeTask task, boolean wasLate, RepositoryCallback<Void> callback) {
        getUserProfile(userId, new RepositoryCallback<>() {
            @Override
            public void onSuccess(UserProfile profile) {
                if (profile == null) {
                    callback.onError("User profile not found");
                    return;
                }

                // Deduct XP
                long xpReward = XpUtil.getXpReward(task.getTaskType(), wasLate);
                profile.setXp(Math.max(0, profile.getXp() - xpReward));
                profile.setLevel(XpUtil.calculateLevel(profile.getXp()));

                // Note: We don't deduct streaks because users might have completed other tasks
                // but we could if this was the ONLY task completed today. 
                // However, for simplicity and to prevent frustration, we'll keep the streak.

                firestore.collection("users").document(userId)
                        .set(profile)
                        .addOnSuccessListener(unused -> callback.onSuccess(null))
                        .addOnFailureListener(e -> callback.onError(e.getMessage()));
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    public void updateStreak(String userId, RepositoryCallback<Void> callback) {
        rewardTaskCompletion(userId, new ChallengeTask(), false, callback);
    }

    public void getUserProfiles(List<String> userIds, RepositoryCallback<Map<String, UserProfile>> callback) {
        if (userIds == null || userIds.isEmpty()) {
            callback.onSuccess(new HashMap<>());
            return;
        }

        Map<String, UserProfile> profiles = new HashMap<>();
        List<String> remaining = new ArrayList<>(userIds);
        fetchNextProfile(remaining, profiles, callback);
    }

    private void fetchNextProfile(List<String> remaining,
                                  Map<String, UserProfile> profiles,
                                  RepositoryCallback<Map<String, UserProfile>> callback) {
        if (remaining.isEmpty()) {
            callback.onSuccess(profiles);
            return;
        }

        String userId = remaining.remove(0);
        firestore.collection("users").document(userId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    UserProfile profile = snapshot.toObject(UserProfile.class);
                    if (profile != null) {
                        profile.setId(snapshot.getId());
                        profiles.put(userId, profile);
                    } else {
                        profiles.put(userId, new UserProfile(userId, "Member"));
                    }
                    fetchNextProfile(remaining, profiles, callback);
                })
                .addOnFailureListener(e -> {
                    profiles.put(userId, new UserProfile(userId, "Member"));
                    fetchNextProfile(remaining, profiles, callback);
                });
    }

    public ListenerRegistration listenToNotifications(String userId, RepositoryCallback<List<AppNotification>> callback) {
        return firestore.collection("users").document(userId)
                .collection("notifications")
                .whereEqualTo("read", false)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        callback.onError(error.getMessage());
                        return;
                    }
                    List<AppNotification> list = new ArrayList<>();
                    if (snapshot != null) {
                        for (QueryDocumentSnapshot doc : snapshot) {
                            AppNotification notification = doc.toObject(AppNotification.class);
                            notification.setId(doc.getId());
                            list.add(notification);
                        }
                    }
                    callback.onSuccess(list);
                });
    }

    public void sendNotification(String toUserId, AppNotification notification, RepositoryCallback<Void> callback) {
        firestore.collection("users").document(toUserId)
                .collection("notifications")
                .add(notification)
                .addOnSuccessListener(ref -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void markNotificationRead(String userId, String notificationId) {
        firestore.collection("users").document(userId)
                .collection("notifications").document(notificationId)
                .update("read", true);
    }
}
