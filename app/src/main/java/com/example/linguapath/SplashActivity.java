package com.example.linguapath;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.example.linguapath.databinding.ActivitySplashBinding;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";
    private static final long SPLASH_DELAY_MS = 2000;

    private ActivitySplashBinding binding;
    private final Handler handler = new Handler(Looper.getMainLooper());

    // The job we run after the delay: open the next screen
    private final Runnable goToLanguageSelect = () -> {
        startActivity(new Intent(SplashActivity.this, LanguageSelectActivity.class));
        finish(); // remove splash from the back stack so Back doesn't return here
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");

        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Gentle fade-in
        binding.getRoot().setAlpha(0f);
        binding.getRoot().animate().alpha(1f).setDuration(800);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart - timer started");
        handler.postDelayed(goToLanguageSelect, SPLASH_DELAY_MS);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop - timer cancelled");
        // If the user leaves the app, cancel the timer so it never runs twice
        handler.removeCallbacks(goToLanguageSelect);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }
}