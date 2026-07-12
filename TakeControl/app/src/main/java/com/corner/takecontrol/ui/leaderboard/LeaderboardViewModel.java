package com.corner.takecontrol.ui.leaderboard;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.corner.takecontrol.data.model.UserProfile;
import com.corner.takecontrol.data.repository.RepositoryCallback;
import com.corner.takecontrol.data.repository.UserRepository;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class LeaderboardViewModel extends ViewModel {

    private final UserRepository userRepository;
    private final MutableLiveData<List<UserProfile>> leaderboard = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    
    private String currentScope = "Global";
    private String currentTimeframe = "weeklyXp";
    private UserProfile currentUserProfile;

    public LeaderboardViewModel() {
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
                    loadLeaderboard();
                }

                @Override
                public void onError(String message) {}
            });
        }
    }

    public void setScope(String scope) {
        this.currentScope = scope;
        loadLeaderboard();
    }

    public void setTimeframe(String timeframe) {
        this.currentTimeframe = timeframe;
        loadLeaderboard();
    }

    public void loadLeaderboard() {
        loading.setValue(true);
        RepositoryCallback<List<UserProfile>> callback = new RepositoryCallback<List<UserProfile>>() {
            @Override
            public void onSuccess(List<UserProfile> result) {
                leaderboard.setValue(result);
                loading.setValue(false);
            }

            @Override
            public void onError(String message) {
                error.setValue(message);
                loading.setValue(false);
            }
        };

        switch (currentScope) {
            case "Global":
                userRepository.getGlobalLeaderboard(currentTimeframe, 50, callback);
                break;
            case "Friends":
                if (currentUserProfile != null) {
                    List<String> ids = new ArrayList<>(currentUserProfile.getFriendIds());
                    ids.add(currentUserProfile.getId());
                    userRepository.getFriendsLeaderboard(ids, currentTimeframe, callback);
                } else {
                    leaderboard.setValue(new ArrayList<>());
                    loading.setValue(false);
                }
                break;
            case "Country":
                if (currentUserProfile != null && currentUserProfile.getCountry() != null) {
                    userRepository.getFilteredLeaderboard("country", currentUserProfile.getCountry(), currentTimeframe, 50, callback);
                } else {
                    leaderboard.setValue(new ArrayList<>());
                    loading.setValue(false);
                }
                break;
            case "University":
                if (currentUserProfile != null && currentUserProfile.getUniversity() != null) {
                    userRepository.getFilteredLeaderboard("university", currentUserProfile.getUniversity(), currentTimeframe, 50, callback);
                } else {
                    leaderboard.setValue(new ArrayList<>());
                    loading.setValue(false);
                }
                break;
            case "Company":
                if (currentUserProfile != null && currentUserProfile.getCompany() != null) {
                    userRepository.getFilteredLeaderboard("company", currentUserProfile.getCompany(), currentTimeframe, 50, callback);
                } else {
                    leaderboard.setValue(new ArrayList<>());
                    loading.setValue(false);
                }
                break;
            default:
                loading.setValue(false);
                break;
        }
    }

    public LiveData<List<UserProfile>> getLeaderboard() { return leaderboard; }
    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<String> getError() { return error; }
    public String getCurrentTimeframe() { return currentTimeframe; }
}
