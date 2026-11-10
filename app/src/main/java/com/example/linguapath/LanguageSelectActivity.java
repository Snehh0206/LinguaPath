package com.example.linguapath;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.animation.ObjectAnimator;

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
    private ObjectAnimator floatKorean;
    private ObjectAnimator floatSpanish;

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
        binding.cardTopics.setOnClickListener(v ->
                startActivity(new Intent(this, TopicsActivity.class)));
        Motion.fadeSlideIn(binding.cardTopics, 340);
        TouchBounce.attach(binding.cardTopics);
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
            if (item.getItemId() == R.id.menu_chat) {
                startActivity(new Intent(this, ChatRoomsActivity.class));
                return true;
            }
            if (item.getItemId() == R.id.menu_progress) {
                startActivity(new Intent(this, ProgressActivity.class));
                return true;
            }
            if (item.getItemId() == R.id.menu_translator) {
                startActivity(new Intent(this, TranslatorActivity.class));
                return true;
            }
            if (item.getItemId() == R.id.menu_saved) {
                startActivity(new Intent(this, SavedWordsActivity.class));
                return true;
            }
            if (item.getItemId() == R.id.menu_reminder) {
                startActivity(new Intent(this, ReminderActivity.class));
                return true;
            }
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
                    //stopService(new Intent(this, com.example.linguapath.service.StudySessionService.class));
                    com.example.linguapath.service.StudyTimerController.endSession();
                    authRepository.logout();
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }


    @Override
    protected void onResume() {
        super.onResume();
        binding.scenery.start();
        // The cards hover gently, one slightly after the other
        if (floatKorean == null) {
            floatKorean = Motion.startFloat(binding.cardKorean, 3, 900);
            floatSpanish = Motion.startFloat(binding.cardSpanish, 3, 1300);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        binding.scenery.stop();
        Motion.stopFloat(floatKorean, binding.cardKorean);
        Motion.stopFloat(floatSpanish, binding.cardSpanish);
        floatKorean = null;
        floatSpanish = null;
    }


}