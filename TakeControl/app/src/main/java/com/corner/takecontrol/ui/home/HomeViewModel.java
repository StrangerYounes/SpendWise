package com.corner.takecontrol.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.corner.takecontrol.data.model.Challenge;
import com.corner.takecontrol.data.repository.ChallengeRepository;
import com.corner.takecontrol.data.repository.RepositoryCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

public class HomeViewModel extends ViewModel {

    private final ChallengeRepository challengeRepository;
    private final MutableLiveData<List<Challenge>> challenges = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(true);
    private ListenerRegistration challengesListener;

    public HomeViewModel() {
        challengeRepository = new ChallengeRepository();
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

    public void startListening() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            loading.setValue(false);
            return;
        }
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (challengesListener != null) {
            challengesListener.remove();
        }
        loading.setValue(true);
        challengesListener = challengeRepository.listenToMyChallenges(userId, new RepositoryCallback<List<Challenge>>() {
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
