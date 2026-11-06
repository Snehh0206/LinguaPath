package com.example.linguapath;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.linguapath.data.AuthRepository;
import com.example.linguapath.databinding.ActivityLoginBinding;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authRepository = new AuthRepository();

        binding.sprig.setOnClickListener(v -> binding.sprig.react());
        binding.buttonLogin.setOnClickListener(v -> attemptLogin());
        binding.textGoRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        binding.sprig.startIdle();
    }

    @Override
    protected void onPause() {
        super.onPause();
        binding.sprig.stopIdle();
    }

    private void attemptLogin() {
        String email = binding.editEmail.getText() == null
                ? "" : binding.editEmail.getText().toString().trim();
        String password = binding.editPassword.getText() == null
                ? "" : binding.editPassword.getText().toString();

        binding.inputLayoutEmail.setError(null);
        binding.inputLayoutPassword.setError(null);
        binding.textAuthError.setText("");

        // Validate first, so we don't call the network for obvious mistakes
        boolean valid = true;
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.inputLayoutEmail.setError(getString(R.string.error_email_invalid));
            valid = false;
        }
        if (password.isEmpty()) {
            binding.inputLayoutPassword.setError(getString(R.string.error_password_empty));
            valid = false;
        }
        if (!valid) {
            return;
        }

        setLoading(true);
        authRepository.login(email, password, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess() {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                openApp();
            }

            @Override
            public void onError(int messageResId) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                setLoading(false);
                binding.textAuthError.setText(messageResId);
            }
        });
    }

    // While waiting: show the spinner and block extra taps
    private void setLoading(boolean loading) {
        binding.progressAuth.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.buttonLogin.setEnabled(!loading);
    }

    private void openApp() {
        Intent intent = new Intent(this, LanguageSelectActivity.class);
        // Clear the back stack so Back never returns to the login screen
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}