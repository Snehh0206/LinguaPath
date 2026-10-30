package com.example.linguapath;

import android.content.Intent;
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

        binding.cardKorean.setOnClickListener(v -> onLanguagePicked(v, "Korean"));
        binding.cardSpanish.setOnClickListener(v -> onLanguagePicked(v, "Spanish"));
    }

    private void onLanguagePicked(View card, String languageName) {
        Log.d(TAG, "Picked: " + languageName);

        // Press animation, then open that language's dashboard
        card.animate()
                .scaleX(0.96f)
                .scaleY(0.96f)
                .setDuration(100)
                .withEndAction(() -> {
                    card.animate().scaleX(1f).scaleY(1f).setDuration(100);

                    Intent intent = new Intent(this, LanguageDashboardActivity.class);
                    intent.putExtra(LanguageDashboardActivity.EXTRA_LANGUAGE, languageName);
                    startActivity(intent);
                });
    }
}