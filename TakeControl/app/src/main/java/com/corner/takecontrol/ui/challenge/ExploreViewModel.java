package com.corner.takecontrol.ui.challenge;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.corner.takecontrol.data.model.Challenge;
import com.corner.takecontrol.data.repository.ChallengeRepository;
import com.corner.takecontrol.data.repository.RepositoryCallback;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;
import java.util.stream.Collectors;

public class ExploreViewModel extends ViewModel {

    private final ChallengeRepository challengeRepository;
    private final MutableLiveData<List<Challenge>> publicChallenges = new MutableLiveData<>();
    private final MutableLiveData<List<Challenge>> filteredChallenges = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<String> challengeJoined = new MutableLiveData<>();

    public ExploreViewModel() {
        challengeRepository = new ChallengeRepository();
        loadPublicChallenges();
    }

    public LiveData<List<Challenge>> getFilteredChallenges() {
        return filteredChallenges;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<String> getError() {
        return error;
    }

    public LiveData<String> getChallengeJoined() {
        return challengeJoined;
    }

    public void consumeChallengeJoined() {
        challengeJoined.setValue(null);
    }

    public void loadPublicChallenges() {
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) return;

        loading.setValue(true);
        challengeRepository.getPublicChallenges(userId, new RepositoryCallback<>() {
            @Override
            public void onSuccess(List<Challenge> result) {
                loading.setValue(false);
                publicChallenges.setValue(result);
                filteredChallenges.setValue(result);
            }

            @Override
            public void onError(String message) {
                loading.setValue(false);
                error.setValue(message);
            }
        });
    }

    public void filter(String query) {
        List<Challenge> all = publicChallenges.getValue();
        if (all == null) return;

        if (query == null || query.isEmpty()) {
            filteredChallenges.setValue(all);
        } else {
            String lowerQuery = query.toLowerCase();
            List<Challenge> filtered = all.stream()
                    .filter(c -> c.getTitle().toLowerCase().contains(lowerQuery) ||
                            (c.getDescription() != null && c.getDescription().toLowerCase().contains(lowerQuery)))
                    .collect(Collectors.toList());
            filteredChallenges.setValue(filtered);
        }
    }

    public void joinChallenge(String challengeId) {
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) return;

        loading.setValue(true);
        challengeRepository.joinChallenge(userId, challengeId, new RepositoryCallback<>() {
            @Override
            public void onSuccess(String result) {
                loading.setValue(false);
                challengeJoined.setValue(result);
            }

            @Override
            public void onError(String message) {
                loading.setValue(false);
                error.setValue(message);
            }
        });
    }
}
