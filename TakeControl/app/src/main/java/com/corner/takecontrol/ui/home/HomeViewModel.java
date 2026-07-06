package com.corner.takecontrol.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.corner.takecontrol.data.model.Challenge;
import com.corner.takecontrol.data.repository.ChallengeRepository;
import com.corner.takecontrol.data.repository.RepositoryCallback;
import com.corner.takecontrol.data.repository.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

public class HomeViewModel extends ViewModel {

    private final ChallengeRepository challengeRepository;
    private final UserRepository userRepository;
    private final MutableLiveData<List<Challenge>> challenges = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(true);
    private final MutableLiveData<String> displayName = new MutableLiveData<>();
    private ListenerRegistration challengesListener;

    public HomeViewModel() {
        challengeRepository = new ChallengeRepository();
        userRepository = new UserRepository();
    }

    public LiveData<List<Challenge>> getChallenges() {
        return challenges;
    }

    public LiveData<String> getError() {
        return error;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<String> getDisplayName() {
        return displayName;
    }

    public void startListening() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            loading.setValue(false);
            return;
        }
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        userRepository.getUserProfile(userId, new RepositoryCallback<>() {
            @Override
            public void onSuccess(com.corner.takecontrol.data.model.UserProfile result) {
                if (result != null && result.getDisplayName() != null) {
                    displayName.setValue(result.getDisplayName());
                }
            }

            @Override
            public void onError(String message) {
                // Non-critical; greeting falls back to default
            }
        });
        if (challengesListener != null) {
            challengesListener.remove();
        }
        loading.setValue(true);
        challengesListener = challengeRepository.listenToMyChallenges(userId, new RepositoryCallback<>() {
            @Override
            public void onSuccess(List<Challenge> result) {
                loading.setValue(false);
                challenges.setValue(result);
            }

            @Override
            public void onError(String message) {
                loading.setValue(false);
                error.setValue(message);
            }
        });
    }

    @Override
    protected void onCleared() {
        if (challengesListener != null) {
            challengesListener.remove();
        }
        super.onCleared();
    }
}
