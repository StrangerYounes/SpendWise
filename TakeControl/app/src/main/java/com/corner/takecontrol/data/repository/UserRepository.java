package com.corner.takecontrol.data.repository;

import com.corner.takecontrol.data.model.UserProfile;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
}
