package com.example.linguapath;

import android.animation.ValueAnimator;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.linguapath.adapter.AttemptAdapter;
import com.example.linguapath.data.ProgressRepository;
import com.example.linguapath.databinding.ActivityProgressBinding;
import com.example.linguapath.databinding.ItemAchievementBinding;
import com.example.linguapath.databinding.ItemLanguageProgressBinding;
import com.example.linguapath.model.ProgressSnapshot;
import com.example.linguapath.view.Motion;
import android.content.Intent;

public class ProgressActivity extends AppCompatActivity {

    private ActivityProgressBinding binding;
    private AttemptAdapter adapter;
    private ValueAnimator xpAnimator;
    private int shownXp = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProgressBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.buttonBack.setOnClickListener(v -> finish());

        adapter = new AttemptAdapter();
        binding.recyclerAttempts.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerAttempts.setAdapter(adapter);
        // The page scrolls as a whole, so the list does not scroll by itself
        binding.recyclerAttempts.setNestedScrollingEnabled(false);
    }

    // Reload every time the screen is shown, so the numbers are never old
    @Override
    protected void onStart() {
        super.onStart();
        ProgressRepository.load(this, this::render);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (xpAnimator != null) {
            xpAnimator.cancel();
        }
    }
    // Explicit Intent: a quiz made only of the words you need to review
    private void startRevision(String language) {
        Intent intent = new Intent(this, QuizActivity.class);
        intent.putExtra(QuizActivity.EXTRA_LANGUAGE, language);
        intent.putExtra(QuizActivity.EXTRA_REVISION, true);
        startActivity(intent);
    }
    private void render(ProgressSnapshot s) {
        if (isFinishing() || isDestroyed()) {
            return;
        }

        if (xpAnimator != null) {
            xpAnimator.cancel();
        }
        xpAnimator = Motion.countUp(shownXp, s.xp, 800,
                value -> binding.textXp.setText(String.valueOf(value)));
        shownXp = s.xp;
        binding.textStreak.setText(String.valueOf(s.streak));
        binding.textToday.setText(String.valueOf(s.studyMinutesToday));

        // One row per language
        binding.groupLanguages.removeAllViews();
        for (ProgressSnapshot.LanguageProgress lp : s.languages) {
            ItemLanguageProgressBinding row = ItemLanguageProgressBinding.inflate(
                    getLayoutInflater(), binding.groupLanguages, false);
            row.textLanguageName.setText(lp.language);
            row.textLanguageCount.setText(getString(R.string.language_stages_count, lp.done, lp.total));
            Motion.animateProgress(row.progressLanguage, lp.done, Math.max(1, lp.total));
            if (lp.revisionCount > 0) {
                row.textRevisionCount.setText(getString(R.string.revise_count, lp.revisionCount));
                row.textRevisionPreview.setText(lp.revisionPreview);
                row.textRevisionPreview.setVisibility(View.VISIBLE);
                row.buttonRevise.setVisibility(View.VISIBLE);
                row.buttonRevise.setOnClickListener(v -> startRevision(lp.language));
            } else {
                row.textRevisionCount.setText(R.string.revise_none);
                row.textRevisionPreview.setVisibility(View.GONE);
                row.buttonRevise.setVisibility(View.GONE);
            }
            binding.groupLanguages.addView(row.getRoot());
        }

        // Achievements: dimmed when still locked
        binding.groupAchievements.removeAllViews();
        for (ProgressSnapshot.Achievement a : s.achievements) {
            ItemAchievementBinding row = ItemAchievementBinding.inflate(
                    getLayoutInflater(), binding.groupAchievements, false);
            row.textAchievementTitle.setText(
                    getString(a.unlocked ? R.string.ach_unlocked : R.string.ach_locked, a.title));
            row.textAchievementDesc.setText(a.description);
            row.getRoot().setAlpha(a.unlocked ? 1f : 0.5f);
            binding.groupAchievements.addView(row.getRoot());
        }

        adapter.setItems(s.attempts);
        binding.textEmptyHistory.setVisibility(s.attempts.isEmpty() ? View.VISIBLE : View.GONE);
    }
}