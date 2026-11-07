package com.example.linguapath;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;

import com.example.linguapath.data.AuthRepository;
import com.example.linguapath.databinding.ActivityLanguageSelectBinding;
import com.example.linguapath.view.Motion;
import com.example.linguapath.view.TouchBounce;

public class LanguageSelectActivity extends AppCompatActivity {

    private static final String TAG = "LanguageSelect";

    private ActivityLanguageSelectBinding binding;
    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");

        binding = ActivityLanguageSelectBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authRepository = new AuthRepository();

        binding.cardKorean.setOnClickListener(v -> onLanguagePicked("Korean"));
        binding.cardSpanish.setOnClickListener(v -> onLanguagePicked("Spanish"));
        binding.buttonMenu.setOnClickListener(this::showAccountMenu);

        // Cards float in, and tilt in 3D when touched
        Motion.fadeSlideIn(binding.cardKorean, 100);
        Motion.fadeSlideIn(binding.cardSpanish, 220);
        TouchBounce.attach(binding.cardKorean);
        TouchBounce.attach(binding.cardSpanish);
    }

    private void onLanguagePicked(String languageName) {
        Log.d(TAG, "Picked: " + languageName);
        Intent intent = new Intent(this, LanguageDashboardActivity.class);
        intent.putExtra(LanguageDashboardActivity.EXTRA_LANGUAGE, languageName);
        startActivity(intent);
    }

    // The three-dots menu: shows who is signed in, and offers Log out
    private void showAccountMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenuInflater().inflate(R.menu.menu_account, popup.getMenu());

        String email = authRepository.getEmail();
        popup.getMenu().findItem(R.id.menu_user)
                .setTitle(getString(R.string.signed_in_as, email == null ? "" : email));

        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.menu_logout) {
                confirmLogout();
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.logout_title)
                .setMessage(R.string.logout_message)
                .setPositiveButton(R.string.menu_logout, (dialog, which) -> {
                    authRepository.logout();
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}