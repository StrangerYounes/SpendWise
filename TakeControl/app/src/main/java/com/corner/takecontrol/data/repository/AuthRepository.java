package com.corner.takecontrol.data.repository;

import com.corner.takecontrol.data.model.UserProfile;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

public class AuthRepository {

    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;

    public AuthRepository() {
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
    }

    public boolean isSignedIn() {
        return auth.getCurrentUser() != null;
    }

    public void signInWithEmail(String email, String password, RepositoryCallback<FirebaseUser> callback) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> callback.onSuccess(result.getUser()))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void registerWithEmail(String email, String password, String displayName,
                                  RepositoryCallback<FirebaseUser> callback) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    FirebaseUser user = result.getUser();
                    if (user == null) {
                        callback.onError("Registration failed");
                        return;
                    }
                    UserProfileChangeRequest profileUpdate = new UserProfileChangeRequest.Builder()
                            .setDisplayName(displayName)
                            .build();
                    user.updateProfile(profileUpdate)
                            .addOnSuccessListener(unused -> createUserProfile(user, displayName, callback))
                            .addOnFailureListener(e -> createUserProfile(user, displayName, callback));
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    private void createUserProfile(FirebaseUser user, String displayName, RepositoryCallback<FirebaseUser> callback) {
        UserProfile profile = new UserProfile(user.getUid(), displayName);
        firestore.collection("users").document(user.getUid())
                .set(profile)
                .addOnSuccessListener(unused -> callback.onSuccess(user))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void signOut() {
        auth.signOut();
    }
}
