package com.corner.takecontrol.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.corner.takecontrol.data.model.Challenge;
import com.corner.takecontrol.data.model.UserProfile;
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
    private final MutableLiveData<Integer> currentStreak = new MutableLiveData<>(0);
    private final MutableLiveData<UserProfile> userProfile = new MutableLiveData<>();
    private ListenerRegistration challengesListener;
    private ListenerRegistration userListener;

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

    public LiveData<Integer> getCurrentStreak() {
        return currentStreak;
    }

    public LiveData<UserProfile> getUserProfile() {
        return userProfile;
    }

    public void startListening() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            loading.setValue(false);
            return;
        }
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        
        if (userListener != null) userListener.remove();
        userListener = userRepository.listenToUserProfile(userId, new RepositoryCallback<>() {
            @Override
            public void onSuccess(com.corner.takecontrol.data.model.UserProfile result) {
                if (result != null) {
                    userProfile.setValue(result);
                    if (result.getDisplayName() != null) {
                        displayName.setValue(result.getDisplayName());
                    }
                    
                    int streak = result.getCurrentStreak();
                    String lastDate = result.getLastCompletionDate();
                    if (lastDate != null) {
                        java.util.Calendar cal = java.util.Calendar.getInstance();
                        String today = String.format(java.util.Locale.US, "%04d-%02d-%02d",
                                cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH) + 1, cal.get(java.util.Calendar.DAY_OF_MONTH));
                        cal.add(java.util.Calendar.DAY_OF_YEAR, -1);
                        String yesterday = String.format(java.util.Locale.US, "%04d-%02d-%02d",
                                cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH) + 1, cal.get(java.util.Calendar.DAY_OF_MONTH));

                        if (!today.equals(lastDate) && !yesterday.equals(lastDate)) {
                            streak = 0;
                        }
                    }
                    currentStreak.setValue(streak);
                }
            }

            @Override
            public void onError(String message) {
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
        if (userListener != null) {
            userListener.remove();
        }
        super.onCleared();
    }
}
