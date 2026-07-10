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

    public void uploadAndSetPhoto(android.net.Uri uri, android.content.ContentResolver contentResolver) {
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null || uri == null) return;

        loading.setValue(true);
        new Thread(() -> {
            try {
                java.io.InputStream inputStream = contentResolver.openInputStream(uri);
                if (inputStream == null) {
                    loading.postValue(false);
                    error.postValue("Could not open image");
                    return;
                }

                // 1. Load and Resize Bitmap to ensure it stays well under the 1MB Firestore limit
                // TODO: If using Firebase Storage, you can upload high-res images here instead of resizing.
                android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(inputStream);
                inputStream.close();
                
                if (bitmap == null) {
                    loading.postValue(false);
                    error.postValue("Could not decode image");
                    return;
                }

                // Resize to max 200x200
                int width = bitmap.getWidth();
                int height = bitmap.getHeight();
                float ratio = (float) width / height;
                int newWidth = 200;
                int newHeight = (int) (200 / ratio);
                if (ratio < 1) {
                    newHeight = 200;
                    newWidth = (int) (200 * ratio);
                }
                
                android.graphics.Bitmap resized = android.graphics.Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                resized.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, baos);
                byte[] imageBytes = baos.toByteArray();

                // 2. Encrypt the resized bytes
                byte[] encrypted = com.corner.takecontrol.util.SecurityUtil.encryptData(imageBytes);
                String base64Encrypted = android.util.Base64.encodeToString(encrypted, android.util.Base64.NO_WRAP);

                // 3. Update Profile locally and then sync to Firestore
                UserProfile profile = userProfile.getValue();
                if (profile != null) {
                    profile.setEncryptedPhoto(base64Encrypted);
                    profile.setPhotoUrl(null); // Clear storage URL if any
                    
                    // Run update on main thread for LiveData
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        updateProfile(profile);
                        loading.setValue(false);
                    });
                } else {
                    loading.postValue(false);
                }
            } catch (Exception e) {
                loading.postValue(false);
                error.postValue(e.getMessage());
            }
        }).start();
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
