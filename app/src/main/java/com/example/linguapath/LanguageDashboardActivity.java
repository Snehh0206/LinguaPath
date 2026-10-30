package com.example.linguapath;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.linguapath.adapter.StageAdapter;
import com.example.linguapath.data.ProgressStore;
import com.example.linguapath.data.StageRepository;
import com.example.linguapath.databinding.ActivityLanguageDashboardBinding;
import com.example.linguapath.model.Stage;

import java.util.List;

public class LanguageDashboardActivity extends AppCompatActivity {

    public static final String EXTRA_LANGUAGE = "extra_language";
    private static final String TAG = "LanguageDashboard";

    private ActivityLanguageDashboardBinding binding;
    private ProgressStore progressStore;
    private StageAdapter adapter;
    private List<Stage> stages;
    private String language;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");

        binding = ActivityLanguageDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Which language did the user pick? It comes from the Intent extra.
        language = getIntent().getStringExtra(EXTRA_LANGUAGE);
        if (language == null) {
            language = "Spanish";
        }

        progressStore = new ProgressStore(this);
        stages = StageRepository.getStages(language);

        binding.textLanguageTitle.setText(getString(R.string.dashboard_title, language));

        adapter = new StageAdapter(stages, this::onStageClicked);
        binding.recyclerStages.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerStages.setAdapter(adapter);

        binding.buttonChangeLanguage.setOnClickListener(v -> finish());
    }

    // Runs every time the screen comes back, so progress is always fresh
    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        refreshProgress();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
    }

    private void refreshProgress() {
        int completed = progressStore.getCompletedCount(language);

        adapter.setCompletedCount(completed);
        binding.progressBar.setMax(stages.size());
        binding.progressBar.setProgress(completed);
        binding.textProgress.setText(
                getString(R.string.dashboard_progress, completed, stages.size()));
        binding.textMessage.setText("");

        // Scroll so the next stage is visible
        binding.recyclerStages.scrollToPosition(Math.min(completed, stages.size() - 1));
    }

    private void onStageClicked(Stage stage, int state) {
        if (state == StageAdapter.STATE_LOCKED) {
            // Inline message instead of a Toast
            binding.textMessage.setText(R.string.stage_locked);
            return;
        }

        // Explicit Intent: we name the exact Activity to open and pass data as extras
        Intent intent = new Intent(this, CourseActivity.class);
        intent.putExtra(EXTRA_LANGUAGE, language);
        intent.putExtra(CourseActivity.EXTRA_STAGE_INDEX, stage.getId());
        intent.putExtra(CourseActivity.EXTRA_STAGE_TITLE, stage.getTitle());
        startActivity(intent);
    }
}