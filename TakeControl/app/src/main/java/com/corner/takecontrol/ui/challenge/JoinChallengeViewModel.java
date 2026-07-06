package com.corner.takecontrol.ui.challenge;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.corner.takecontrol.data.repository.ChallengeRepository;
import com.corner.takecontrol.data.repository.RepositoryCallback;
import com.google.firebase.auth.FirebaseAuth;

public class JoinChallengeViewModel extends ViewModel {

    private final ChallengeRepository challengeRepository;
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<String> joinedChallengeId = new MutableLiveData<>();

    public JoinChallengeViewModel() {
        challengeRepository = new ChallengeRepository();
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<String> getError() {
        return error;
    }

    public LiveData<String> getJoinedChallengeId() {
        return joinedChallengeId;
    }

    public void joinChallenge(String shareCode) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            error.setValue("You must be signed in");
            return;
        }
        if (shareCode == null || shareCode.trim().isEmpty()) {
            error.setValue("Enter a share code");
            return;
        }

        loading.setValue(true);
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        challengeRepository.joinByShareCode(userId, shareCode, new RepositoryCallback<>() {
            @Override
            public void onSuccess(String challengeId) {
                loading.setValue(false);
                joinedChallengeId.setValue(challengeId);
            }

            @Override
            public void onError(String message) {
                loading.setValue(false);
                error.setValue(message);
            }
        });
    }
}
