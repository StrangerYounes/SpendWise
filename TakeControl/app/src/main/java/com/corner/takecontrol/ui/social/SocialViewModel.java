package com.corner.takecontrol.ui.social;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.corner.takecontrol.data.model.AppNotification;
import com.corner.takecontrol.data.model.UserProfile;
import com.corner.takecontrol.data.repository.RepositoryCallback;
import com.corner.takecontrol.data.repository.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SocialViewModel extends ViewModel {

    private final UserRepository userRepository;
    private final MutableLiveData<List<UserProfile>> users = new MutableLiveData<>();
    private final MutableLiveData<List<AppNotification>> requests = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    
    private UserProfile currentUserProfile;

    public SocialViewModel() {
        userRepository = new UserRepository();
        loadCurrentUser();
    }

    private void loadCurrentUser() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            userRepository.getUserProfile(uid, new RepositoryCallback<UserProfile>() {
                @Override
                public void onSuccess(UserProfile profile) {
                    currentUserProfile = profile;
                    loadFriends();
                }
                @Override public void onError(String message) { error.setValue(message); }
            });
        }
    }

    public void loadFriends() {
        if (currentUserProfile == null) return;
        loading.setValue(true);
        userRepository.getUserProfiles(currentUserProfile.getFriendIds(), new RepositoryCallback<Map<String, UserProfile>>() {
            @Override
            public void onSuccess(Map<String, UserProfile> result) {
                users.setValue(new ArrayList<>(result.values()));
                loading.setValue(false);
            }
            @Override
            public void onError(String message) {
                error.setValue(message);
                loading.setValue(false);
            }
        });
    }

    public void searchUsers(String query) {
        loading.setValue(true);
        userRepository.searchUsers(query, new RepositoryCallback<List<UserProfile>>() {
            @Override
            public void onSuccess(List<UserProfile> result) {
                // Filter out current user
                String myId = FirebaseAuth.getInstance().getUid();
                List<UserProfile> filtered = new ArrayList<>();
                for (UserProfile u : result) {
                    if (!u.getId().equals(myId)) filtered.add(u);
                }
                
                if (filtered.isEmpty()) {
                    error.setValue("No users found. Remember search is case-sensitive!");
                }

                users.setValue(filtered);
                loading.setValue(false);
            }
            @Override
            public void onError(String message) {
                error.setValue(message);
                loading.setValue(false);
            }
        });
    }

    public void loadRequests() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;
        loading.setValue(true);
        
        FirebaseFirestore.getInstance().collection("users").document(uid).collection("notifications")
                .whereEqualTo("type", "FRIEND_REQUEST")
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<AppNotification> list = new ArrayList<>();
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : snapshot) {
                        AppNotification n = doc.toObject(AppNotification.class);
                        n.setId(doc.getId());
                        list.add(n);
                    }
                    requests.setValue(list);
                    loading.setValue(false);
                })
                .addOnFailureListener(e -> {
                    error.setValue(e.getMessage());
                    loading.setValue(false);
                });
    }

    public void sendFriendRequest(UserProfile targetUser) {
        if (currentUserProfile == null) return;
        userRepository.sendFriendRequest(currentUserProfile.getId(), currentUserProfile.getDisplayName(), targetUser.getId(), new RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                if (currentUserProfile.getSentRequestIds() == null) {
                    currentUserProfile.setSentRequestIds(new ArrayList<>());
                }
                currentUserProfile.getSentRequestIds().add(targetUser.getId());
                // Trigger an update to the observers
                users.setValue(users.getValue());
            }
            @Override public void onError(String message) { error.setValue(message); }
        });
    }

    public void cancelFriendRequest(UserProfile targetUser) {
        if (currentUserProfile == null) return;
        userRepository.cancelFriendRequest(currentUserProfile.getId(), targetUser.getId(), new RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                if (currentUserProfile.getSentRequestIds() != null) {
                    currentUserProfile.getSentRequestIds().remove(targetUser.getId());
                }
                // Trigger an update
                users.setValue(users.getValue());
            }
            @Override public void onError(String message) { error.setValue(message); }
        });
    }

    public void acceptRequest(AppNotification notification) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;
        userRepository.acceptFriendRequest(uid, notification.getFromUserId(), notification.getId(), new RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                loadRequests(); // Refresh
                loadCurrentUser(); // To update friendIds
            }
            @Override public void onError(String message) { error.setValue(message); }
        });
    }

    public void declineRequest(AppNotification notification) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;
        FirebaseFirestore.getInstance().collection("users").document(uid).collection("notifications").document(notification.getId())
                .delete()
                .addOnSuccessListener(v -> loadRequests());
    }

    public void removeFriend(String friendId) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;
        userRepository.removeFriend(uid, friendId, new RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                loadCurrentUser();
            }
            @Override public void onError(String message) { error.setValue(message); }
        });
    }

    public LiveData<List<UserProfile>> getUsers() { return users; }
    public LiveData<List<AppNotification>> getRequests() { return requests; }
    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<String> getError() { return error; }
    public UserProfile getCurrentUserProfile() { return currentUserProfile; }
}
