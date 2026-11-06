package com.example.linguapath.data;

import androidx.annotation.StringRes;

import com.example.linguapath.R;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;

// The only class that talks to Firebase Authentication.
// Passwords are sent straight to Firebase over HTTPS. We never store or log them.
public class AuthRepository {

    public interface AuthCallback {
        void onSuccess();
        void onError(@StringRes int messageResId);
    }

    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    // Firebase keeps the user signed in between app launches by itself
    public boolean isLoggedIn() {
        return auth.getCurrentUser() != null;
    }

    public String getEmail() {
        FirebaseUser user = auth.getCurrentUser();
        return (user == null) ? null : user.getEmail();
    }

    public void register(String email, String password, AuthCallback callback) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess();
                    } else {
                        callback.onError(errorFor(task.getException()));
                    }
                });
    }

    public void login(String email, String password, AuthCallback callback) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess();
                    } else {
                        callback.onError(errorFor(task.getException()));
                    }
                });
    }

    public void logout() {
        auth.signOut();
    }

    // Turns Firebase's technical errors into friendly messages
    @StringRes
    private int errorFor(Exception e) {
        if (e instanceof FirebaseAuthWeakPasswordException) {
            return R.string.error_weak_password;
        }
        if (e instanceof FirebaseAuthUserCollisionException) {
            return R.string.error_email_taken;
        }
        if (e instanceof FirebaseAuthInvalidCredentialsException
                || e instanceof FirebaseAuthInvalidUserException) {
            return R.string.error_wrong_credentials;
        }
        if (e instanceof FirebaseNetworkException) {
            return R.string.error_no_network;
        }
        if (e instanceof FirebaseTooManyRequestsException) {
            return R.string.error_too_many;
        }
        return R.string.error_generic;
    }
}