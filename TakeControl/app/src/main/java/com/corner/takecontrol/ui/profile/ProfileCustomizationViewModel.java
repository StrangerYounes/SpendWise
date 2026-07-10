package com.corner.takecontrol.ui.profile;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.corner.takecontrol.data.model.UserProfile;
import com.corner.takecontrol.data.repository.RepositoryCallback;
import com.corner.takecontrol.data.repository.UserRepository;
import com.google.firebase.auth.FirebaseAuth;

public class ProfileCustomizationViewModel extends ViewModel {

    private final UserRepository userRepository;
    private final MutableLiveData<UserProfile> userProfile = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public ProfileCustomizationViewModel() {
        userRepository = new UserRepository();
        loadProfile();
    }

    public LiveData<UserProfile> getUserProfile() {
        return userProfile;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<String> getError() {
        return error;
    }

    public void loadProfile() {
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) return;

        loading.setValue(true);
        userRepository.getUserProfile(userId, new RepositoryCallback<>() {
            @Override
            public void onSuccess(UserProfile result) {
                if (result != null) {
                    if (com.corner.takecontrol.util.ProgressionUtil.checkAndUnlockRewards(result)) {
                        updateProfile(result);
                    } else {
                        userProfile.setValue(result);
                    }
                }
                loading.setValue(false);
            }

            @Override
            public void onError(String message) {
                loading.setValue(false);
                error.setValue(message);
            }
        });
    }

    public void equipFrame(String frameId) {
        UserProfile profile = userProfile.getValue();
        if (profile == null || (profile.getUnlockedFrames() != null && !profile.getUnlockedFrames().contains(frameId) && !frameId.equals("NONE"))) {
            return;
        }

        profile.setEquippedFrameId(frameId.equals("NONE") ? null : frameId);
        updateProfile(profile);
    }

    public void equipTitle(String titleId) {
        UserProfile profile = userProfile.getValue();
        if (profile == null || (profile.getUnlockedTitles() != null && !profile.getUnlockedTitles().contains(titleId) && !titleId.equals("NONE"))) {
            return;
        }

        profile.setEquippedTitleId(titleId.equals("NONE") ? null : titleId);
        updateProfile(profile);
    }

    public void updateDisplayName(String name) {
        UserProfile profile = userProfile.getValue();
        if (profile == null || name == null || name.trim().isEmpty()) return;

        profile.setDisplayName(name.trim());
        updateProfile(profile);
    }

    public void updatePhotoUrl(String url) {
        UserProfile profile = userProfile.getValue();
        if (profile == null) return;

        profile.setPhotoUrl(url);
        updateProfile(profile);
    }

    private void updateProfile(UserProfile profile) {
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) return;

        userRepository.updateUserProfile(userId, profile, new RepositoryCallback<>() {
            @Override
            public void onSuccess(Void result) {
                userProfile.setValue(profile);
            }

            @Override
            public void onError(String message) {
                error.setValue(message);
            }
        });
    }
}
