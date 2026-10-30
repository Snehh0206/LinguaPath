package com.example.linguapath;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.linguapath.databinding.ActivityLanguageSelectBinding;

public class LanguageSelectActivity extends AppCompatActivity {

    private static final String TAG = "LanguageSelect";

    private ActivityLanguageSelectBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");

        binding = ActivityLanguageSelectBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Click events: each card calls the same method with its own language name
        binding.cardKorean.setOnClickListener(v -> onLanguagePicked(v, "Korean"));
        binding.cardSpanish.setOnClickListener(v -> onLanguagePicked(v, "Spanish"));
    }

    private void onLanguagePicked(View card, String languageName) {
        Log.d(TAG, "Picked: " + languageName);

        // Small "press" animation: shrink a little, then bounce back
        card.animate()
                .scaleX(0.96f)
                .scaleY(0.96f)
                .setDuration(100)
                .withEndAction(() -> card.animate().scaleX(1f).scaleY(1f).setDuration(100));

        // Inline feedback (better than a Toast here)
        binding.textStatus.setText(getString(R.string.status_picked, languageName));
    }
}