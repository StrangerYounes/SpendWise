package com.corner.takecontrol.ui.stats;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.corner.takecontrol.data.model.UserProfile;
import com.corner.takecontrol.data.model.UserStats;
import com.corner.takecontrol.data.repository.RepositoryCallback;
import com.corner.takecontrol.data.repository.StatsRepository;
import com.corner.takecontrol.data.repository.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;

public class StatisticsViewModel extends ViewModel {

    private final StatsRepository statsRepository;
    private final UserRepository userRepository;
    
    private final MutableLiveData<UserStats> stats = new MutableLiveData<>();
    private final MutableLiveData<UserProfile> userProfile = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);

    private ListenerRegistration statsListener;
    private ListenerRegistration profileListener;

    public StatisticsViewModel() {
        statsRepository = new StatsRepository();
        userRepository = new UserRepository();
        startListening();
    }

    public LiveData<UserStats> getStats() {
        return stats;
    }

    public LiveData<UserProfile> getUserProfile() {
        return userProfile;
    }

    public LiveData<String> getError() {
        return error;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    private void startListening() {
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) return;

        loading.setValue(true);

        statsListener = statsRepository.listenToUserStats(userId, new RepositoryCallback<>() {
            @Override
            public void onSuccess(UserStats result) {
                stats.setValue(result);
                loading.setValue(false);
            }

            @Override
            public void onError(String message) {
                error.setValue(message);
                loading.setValue(false);
            }
        });

        profileListener = userRepository.listenToUserProfile(userId, new RepositoryCallback<>() {
            @Override
            public void onSuccess(UserProfile result) {
                userProfile.setValue(result);
            }

            @Override
            public void onError(String message) {
                error.setValue(message);
            }
        });
    }

    @Override
    protected void onCleared() {
        if (statsListener != null) statsListener.remove();
        if (profileListener != null) profileListener.remove();
        super.onCleared();
    }
}
