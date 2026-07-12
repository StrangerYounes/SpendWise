package com.corner.takecontrol.ui.challenge;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.corner.takecontrol.data.repository.ChallengeRepository;
import com.corner.takecontrol.data.repository.RepositoryCallback;
import com.google.firebase.auth.FirebaseAuth;

public class JoinChallengeViewModel extends ViewModel {

    private final ChallengeRepository challengeRepository;
    private final com.corner.takecontrol.data.repository.UserRepository userRepository;
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<String> joinedChallengeId = new MutableLiveData<>();
    private final MutableLiveData<com.corner.takecontrol.util.ProgressionUtil.SlotStatus> slotLimitExceeded = new MutableLiveData<>();

    public JoinChallengeViewModel() {
        challengeRepository = new ChallengeRepository();
        userRepository = new com.corner.takecontrol.data.repository.UserRepository();
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

    public LiveData<com.corner.takecontrol.util.ProgressionUtil.SlotStatus> getSlotLimitExceeded() {
        return slotLimitExceeded;
    }

    public void consumeJoinedChallengeId() {
        joinedChallengeId.setValue(null);
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
            public void onSuccess(String result) {
                if (result.startsWith("CHECK_PASSWORD:")) {
                    String id = result.substring("CHECK_PASSWORD:".length());
                    challengeRepository.getChallenge(id, new RepositoryCallback<>() {
                        @Override
                        public void onSuccess(com.corner.takecontrol.data.model.Challenge challenge) {
                            loading.setValue(false);
                            if (challenge.getEncryptedPassword() == null || challenge.getEncryptedPassword().isEmpty()) {
                                confirmJoin(id);
                            } else {
                                joinedChallengeId.setValue(result); // Pass the raw result to UI to trigger password dialog
                            }
                        }

                        @Override
                        public void onError(String message) {
                            loading.setValue(false);
                            error.setValue(message);
                        }
                    });
                } else {
                    loading.setValue(false);
                    joinedChallengeId.setValue(result);
                }
            }

            @Override
            public void onError(String message) {
                loading.setValue(false);
                error.setValue(message);
            }
        });
    }

    public void confirmJoin(String challengeId) {
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) return;

        loading.setValue(true);
        userRepository.getUserProfile(userId, new RepositoryCallback<>() {
            @Override
            public void onSuccess(com.corner.takecontrol.data.model.UserProfile profile) {
                challengeRepository.getActiveChallengeCount(userId, new RepositoryCallback<>() {
                    @Override
                    public void onSuccess(Integer count) {
                        int maxSlots = com.corner.takecontrol.util.ProgressionUtil.getMaxChallengeSlots(profile);
                        if (count >= maxSlots) {
                            loading.setValue(false);
                            slotLimitExceeded.setValue(new com.corner.takecontrol.util.ProgressionUtil.SlotStatus(
                                    count, maxSlots, com.corner.takecontrol.util.ProgressionUtil.getNextSlotLevel(profile.getLevel()),
                                    profile.isUnlimitedChallengeSlots()
                            ));
                            return;
                        }

                        challengeRepository.joinChallenge(userId, challengeId, new RepositoryCallback<>() {
                            @Override
                            public void onSuccess(String result) {
                                postJoinToFeed(challengeId, profile);
                                loading.setValue(false);
                                joinedChallengeId.setValue(result);
                            }

                            @Override
                            public void onError(String message) {
                                loading.setValue(false);
                                error.setValue(message);
                            }
                        });
                    }

                    @Override
                    public void onError(String message) {
                        loading.setValue(false);
                        error.setValue(message);
                    }
                });
            }

            @Override
            public void onError(String message) {
                loading.setValue(false);
                error.setValue(message);
            }
        });
    }

    private void postJoinToFeed(String challengeId, com.corner.takecontrol.data.model.UserProfile profile) {
        com.corner.takecontrol.data.model.ChallengePost post = new com.corner.takecontrol.data.model.ChallengePost(
                challengeId, profile.getId(), profile.getDisplayName(), "joined the challenge!", "STATUS"
        );
        post.setUserEncryptedPhoto(profile.getEncryptedPhoto());
        post.setUserPhotoUrl(profile.getPhotoUrl());
        challengeRepository.postToChallengeFeed(challengeId, post, null);
    }
}
