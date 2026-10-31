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

import java.util.ArrayList;
import java.util.List;

public class LanguageDashboardActivity extends AppCompatActivity {

    public static final String EXTRA_LANGUAGE = "extra_language";
    private static final String TAG = "LanguageDashboard";

    private ActivityLanguageDashboardBinding binding;
    private ProgressStore progressStore;
    private StageAdapter adapter;
    private List<Stage> stages = new ArrayList<>();
    private String language;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");

        binding = ActivityLanguageDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        language = getIntent().getStringExtra(EXTRA_LANGUAGE);
        if (language == null) {
            language = "Spanish";
        }

        progressStore = new ProgressStore(this);
        binding.textLanguageTitle.setText(getString(R.string.dashboard_title, language));

        // Start with an empty list; the real stages arrive from the database
        adapter = new StageAdapter(stages, this::onStageClicked);
        binding.recyclerStages.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerStages.setAdapter(adapter);

        binding.buttonChangeLanguage.setOnClickListener(v -> finish());

        StageRepository.loadStages(this, language, loadedStages -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            stages = loadedStages;
            adapter.setStages(stages);
            refreshProgress();
        });
    }

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
        if (stages.isEmpty()) {
            return; // data not loaded yet
        }
        int completed = progressStore.getCompletedCount(language);

        adapter.setCompletedCount(completed);
        binding.progressBar.setMax(stages.size());
        binding.progressBar.setProgress(completed);
        binding.textProgress.setText(
                getString(R.string.dashboard_progress, completed, stages.size()));
        binding.textMessage.setText("");

        binding.recyclerStages.scrollToPosition(Math.min(completed, stages.size() - 1));
    }

    private void onStageClicked(Stage stage, int state) {
        if (state == StageAdapter.STATE_LOCKED) {
            binding.textMessage.setText(R.string.stage_locked);
            return;
        }

        Intent intent = new Intent(this, CourseActivity.class);
        intent.putExtra(EXTRA_LANGUAGE, language);
        intent.putExtra(CourseActivity.EXTRA_STAGE_ID, stage.getId());
        intent.putExtra(CourseActivity.EXTRA_STAGE_INDEX, stage.getOrderIndex());
        intent.putExtra(CourseActivity.EXTRA_STAGE_TITLE, stage.getTitle());
        startActivity(intent);
    }
}