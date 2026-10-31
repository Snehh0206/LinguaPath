package com.example.linguapath;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.appcompat.app.AlertDialog;
import android.view.animation.OvershootInterpolator;

import com.example.linguapath.adapter.StageAdapter;
import com.example.linguapath.data.ProgressStore;
import com.example.linguapath.data.StageRepository;
import com.example.linguapath.databinding.ActivityLanguageDashboardBinding;
import com.example.linguapath.model.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LanguageDashboardActivity extends AppCompatActivity {

    public static final String EXTRA_LANGUAGE = "extra_language";
    private static final String TAG = "LanguageDashboard";

    private ActivityLanguageDashboardBinding binding;
    private ProgressStore progressStore;
    private StageAdapter adapter;
    private List<Stage> stages = new ArrayList<>();
    private String language;
    private final Random random = new Random();
    private int lastCompleted = -1; // -1 = not known yet

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

        // Tap Sprig: it jumps and smiles
        binding.sprig.setOnClickListener(v -> {
            binding.sprig.react();
            showSprigLine(pickSprigLine());
        });

        // Floating "Continue" button: slides up, then jumps to the next unfinished stage
        binding.fabContinue.setOnClickListener(v -> continueLearning());
        binding.fabContinue.setTranslationY(300f);
        binding.fabContinue.animate()
                .translationY(0f)
                .setStartDelay(400)
                .setDuration(600)
                .setInterpolator(new OvershootInterpolator());

        StageRepository.loadStages(this, language, loadedStages -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            stages = loadedStages;
            adapter.setStages(stages);
            refreshProgress();
            showSprigLine(pickSprigLine());
        });
    }
    private void continueLearning() {
        if (stages.isEmpty()) {
            return; // data not loaded yet
        }
        int completed = progressStore.getCompletedCount(language);
        if (completed >= stages.size()) {
            binding.textMessage.setText(R.string.fab_all_done);
            return;
        }
        openStage(stages.get(completed), false);
    }
    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        refreshProgress();
        binding.sprig.startIdle();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        binding.sprig.stopIdle();
    }

    private void refreshProgress() {
        if (stages.isEmpty()) {
            return; // data not loaded yet
        }
        int completed = progressStore.getCompletedCount(language);

        // Coming back after finishing a stage? Sprig celebrates.
        if (lastCompleted >= 0 && completed > lastCompleted) {
            binding.sprig.react();
            showSprigLine(getString(R.string.sprig_stage_done));
        }
        lastCompleted = completed;
        adapter.setCompletedCount(completed);
        binding.progressBar.setMax(stages.size());
        binding.progressBar.setProgress(completed);
        binding.textProgress.setText(
                getString(R.string.dashboard_progress, completed, stages.size()));
        binding.textMessage.setText("");

        binding.recyclerStages.scrollToPosition(Math.min(completed, stages.size() - 1));
    }
    // Chooses what Sprig says, based on the user's progress
    private String pickSprigLine() {
        if (stages.isEmpty()) {
            return getString(R.string.sprig_cheer);
        }
        int completed = progressStore.getCompletedCount(language);
        if (completed >= stages.size()) {
            return getString(R.string.sprig_all_done);
        }

        int pick = random.nextInt(3);
        if (pick == 0) {
            return getString(language.equals("Korean")
                    ? R.string.sprig_hello_korean
                    : R.string.sprig_hello_spanish);
        } else if (pick == 1) {
            return getString(R.string.sprig_next, stages.get(completed).getTitle());
        }
        return getString(R.string.sprig_cheer);
    }

    private void showSprigLine(String line) {
        binding.textSprigSays.setText(line);
        binding.textSprigSays.setAlpha(0f);
        binding.textSprigSays.animate().alpha(1f).setDuration(250);
    }
    private void onStageClicked(Stage stage, int state) {
        if (state == StageAdapter.STATE_LOCKED) {
            binding.textMessage.setText(R.string.stage_locked);
            return;
        }

        if (state == StageAdapter.STATE_DONE) {
            // Finished before: ask if the user wants to redo it
            new AlertDialog.Builder(this)
                    .setTitle(R.string.redo_dialog_title)
                    .setMessage(R.string.redo_dialog_message)
                    .setPositiveButton(R.string.redo_yes, (dialog, which) -> openStage(stage, true))
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
            return;
        }

        openStage(stage, false);
    }

    // Explicit Intent. The "redo" extra tells the course screen which mode to use.
    private void openStage(Stage stage, boolean redo) {
        Intent intent = new Intent(this, CourseActivity.class);
        intent.putExtra(EXTRA_LANGUAGE, language);
        intent.putExtra(CourseActivity.EXTRA_STAGE_ID, stage.getId());
        intent.putExtra(CourseActivity.EXTRA_STAGE_INDEX, stage.getOrderIndex());
        intent.putExtra(CourseActivity.EXTRA_STAGE_TITLE, stage.getTitle());
        intent.putExtra(CourseActivity.EXTRA_REDO, redo);
        startActivity(intent);
    }
}