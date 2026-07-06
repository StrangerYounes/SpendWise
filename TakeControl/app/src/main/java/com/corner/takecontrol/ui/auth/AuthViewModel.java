package com.corner.takecontrol.ui.auth;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.corner.takecontrol.data.repository.AuthRepository;
import com.corner.takecontrol.data.repository.RepositoryCallback;
import com.google.firebase.auth.FirebaseUser;

public class AuthViewModel extends ViewModel {

    private final AuthRepository authRepository;
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<FirebaseUser> authSuccess = new MutableLiveData<>();

    public AuthViewModel() {
        authRepository = new AuthRepository();
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<String> getError() {
        return error;
    }

    public LiveData<FirebaseUser> getAuthSuccess() {
        return authSuccess;
    }

    public boolean isSignedIn() {
        return authRepository.isSignedIn();
    }

    public void signIn(String email, String password) {
        loading.setValue(true);
        authRepository.signInWithEmail(email, password, new RepositoryCallback<>() {
            @Override
            public void onSuccess(FirebaseUser result) {
                loading.setValue(false);
                authSuccess.setValue(result);
            }

            @Override
            public void onError(String message) {
                loading.setValue(false);
                error.setValue(message);
            }
        });
    }

    public void register(String email, String password, String displayName) {
        loading.setValue(true);
        authRepository.registerWithEmail(email, password, displayName, new RepositoryCallback<>() {
            @Override
            public void onSuccess(FirebaseUser result) {
                loading.setValue(false);
                authSuccess.setValue(result);
            }

            @Override
            public void onError(String message) {
                loading.setValue(false);
                error.setValue(message);
            }
        });
    }

    public void clearError() {
        error.setValue(null);
    }
}
