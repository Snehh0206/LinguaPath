package com.example.linguapath;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.linguapath.data.AuthRepository;
import com.example.linguapath.databinding.ActivityRegisterBinding;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authRepository = new AuthRepository();

        binding.buttonRegister.setOnClickListener(v -> attemptRegister());
        binding.textGoLogin.setOnClickListener(v -> finish()); // back to the login screen
    }

    private void attemptRegister() {
        String email = binding.editEmail.getText() == null
                ? "" : binding.editEmail.getText().toString().trim();
        String password = binding.editPassword.getText() == null
                ? "" : binding.editPassword.getText().toString();
        String confirm = binding.editConfirm.getText() == null
                ? "" : binding.editConfirm.getText().toString();

        binding.inputLayoutEmail.setError(null);
        binding.inputLayoutPassword.setError(null);
        binding.inputLayoutConfirm.setError(null);
        binding.textAuthError.setText("");

        boolean valid = true;
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.inputLayoutEmail.setError(getString(R.string.error_email_invalid));
            valid = false;
        }
        if (password.length() < 6) { // Firebase needs at least 6 characters
            binding.inputLayoutPassword.setError(getString(R.string.error_password_short));
            valid = false;
        }
        if (!password.equals(confirm)) {
            binding.inputLayoutConfirm.setError(getString(R.string.error_password_mismatch));
            valid = false;
        }
        if (!valid) {
            return;
        }

        setLoading(true);
        authRepository.register(email, password, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess() {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                // Firebase signs the new user in automatically
                Intent intent = new Intent(RegisterActivity.this, LanguageSelectActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
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

    private void setLoading(boolean loading) {
        binding.progressAuth.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.buttonRegister.setEnabled(!loading);
    }
}