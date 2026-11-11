package com.example.linguapath;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.linguapath.adapter.StageAdapter;
import com.example.linguapath.data.CloudSyncRepository;
import com.example.linguapath.data.ProgressStore;
import com.example.linguapath.data.ReminderSettings;
import com.example.linguapath.data.StageRepository;
import com.example.linguapath.data.WordOfDay;
import com.example.linguapath.data.WordOfDayRepository;
import com.example.linguapath.databinding.ActivityLanguageDashboardBinding;
import com.example.linguapath.model.Stage;
import com.example.linguapath.service.StudyTimerService;
import com.example.linguapath.service.StudyTimerState;
import com.example.linguapath.service.WordOfDayService;
import com.example.linguapath.speech.SpeechHelper;
import com.example.linguapath.view.Motion;
import com.example.linguapath.data.QuizHistoryRepository;

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

    private final CloudSyncRepository cloudSync = new CloudSyncRepository();
    private final Random random = new Random();
    private ValueAnimator xpAnimator;
    private ObjectAnimator fabFloat;
    private int shownXp = 0;
    private int lastCompleted = -1; // -1 = not known yet
    private SpeechHelper speech;
    private WordOfDay todaysWord;

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
        new ReminderSettings(this).setLastLanguage(language); // the reminder opens this path
        binding.textLanguageTitle.setText(getString(R.string.dashboard_title, language));

        // "Today: 12 min 34 s" follows the study timer. There is nothing to tap.
        StudyTimerState.live().observe(this, this::showToday);

        // The stage path. It starts empty. The real stages arrive from the database.
        adapter = new StageAdapter(stages, this::onStageClicked);
        binding.recyclerStages.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerStages.setAdapter(adapter);
        binding.recyclerStages.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                binding.scenery.setScroll(recyclerView.computeVerticalScrollOffset());
            }
        });

        binding.buttonChangeLanguage.setOnClickListener(v -> finish());
        binding.sprig.setOnClickListener(v -> {
            binding.sprig.react();
            showSprigLine(pickSprigLine());
        });
        binding.fabContinue.setOnClickListener(v -> continueLearning());
        binding.textSyncStatus.setOnClickListener(v -> syncWithCloud());
        binding.buttonWod.setOnClickListener(v -> showWordOfDay());

        // Things float in one after another
        Motion.fadeSlideIn(binding.textLanguageTitle, 0);
        Motion.fadeSlideIn(binding.textProgress, 80);
        Motion.fadeSlideIn(binding.sprigRow, 160);
        Motion.fadeSlideIn(binding.fabContinue, 500);

        StageRepository.loadStages(this, language, loadedStages -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            stages = loadedStages;
            adapter.setStages(stages);
            refreshProgress();
            showSprigLine(pickSprigLine());
        });

        syncWithCloud();
    }

    @Override
    protected void onStart() {
        super.onStart();
        speech = new SpeechHelper(this);

        // Word of the day: a background service fetches it when it is needed, then stops itself
        WordOfDayService.requestIfNeeded(this);
        loadWordOfDay();
        // The service needs a few seconds, so look again. The card then appears without reopening the screen.
        binding.getRoot().postDelayed(this::loadWordOfDay, 7000);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (speech != null) {
            speech.shutdown();
            speech = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        binding.sprig.startIdle();
        binding.scenery.start();
        if (fabFloat == null) {
            fabFloat = Motion.startFloat(binding.fabContinue, 4, 1400);
        }
        refreshProgress();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        binding.sprig.stopIdle();
        binding.scenery.stop();
        Motion.stopFloat(fabFloat, binding.fabContinue);
        fabFloat = null;
        if (xpAnimator != null) {
            xpAnimator.cancel();
        }
    }

    // ---- Word of the day ----

    private void loadWordOfDay() {
        WordOfDayRepository.loadToday(this, language, word -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            todaysWord = word;
            binding.buttonWod.setVisibility(word == null ? View.GONE : View.VISIBLE);
        });
    }

    // The word pops out in a small dialog, with a Listen button
    private void showWordOfDay() {
        if (todaysWord == null) {
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.wod_label)
                .setMessage(todaysWord.translated + "\n" + todaysWord.english
                        + "\n\n" + getString(R.string.wod_credit))
                .setPositiveButton(R.string.wod_listen, (dialog, which) -> {
                    if (speech != null) {
                        speech.speak(todaysWord.translated, language);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    // ---- Study time ("Today: 12 min 34 s") ----

    private void showToday(StudyTimerState.Snapshot s) {
        long total = progressStore.getStudyMillisToday() + (s.active ? s.unsavedMs : 0);
        binding.textTimer.setText(getString(R.string.today_time, StudyTimerService.formatLong(total)));
    }

    // ---- Progress ----

    private void refreshProgress() {
        if (stages.isEmpty()) {
            return; // data not loaded yet
        }
        final int completed = progressStore.getCompletedCount(language);
        final int total = stages.size();

        // Coming back after finishing a stage? Celebrate.
        if (lastCompleted >= 0 && completed > lastCompleted) {
            binding.sprig.react();
            binding.confetti.burst();
            showSprigLine(getString(R.string.sprig_stage_done));
        }
        lastCompleted = completed;
        // Best quiz score per stage, loaded in the background
        QuizHistoryRepository.loadBestScores(this, language, scores -> {
            if (!isFinishing() && !isDestroyed()) {
                adapter.setBestScores(scores);
            }
        });

        adapter.setCompletedCount(completed);
        Motion.animateProgress(binding.progressBar, completed, total);

        // XP counts up to its new value
        int xp = progressStore.getXp();
        if (xpAnimator != null) {
            xpAnimator.cancel();
        }
        xpAnimator = Motion.countUp(shownXp, xp, 800, value ->
                binding.textProgress.setText(
                        getString(R.string.dashboard_progress_xp, completed, total, value)));
        shownXp = xp;

        binding.textMessage.setText("");
        binding.recyclerStages.scrollToPosition(Math.min(completed, total - 1));
    }

    // ---- Sprig ----

    // Chooses what Sprig says, based on the user's progress
    private String pickSprigLine() {
        if (stages.isEmpty()) {
            return getString(R.string.sprig_cheer);
        }
        int completed = progressStore.getCompletedCount(language);
        if (completed >= stages.size()) {
            return getString(R.string.sprig_all_done);
        }

        long minutes = progressStore.getStudyMillisToday() / 60000;
        int pick = random.nextInt(minutes > 0 ? 4 : 3);
        if (pick == 0) {
            return getString(language.equals("Korean")
                    ? R.string.sprig_hello_korean
                    : R.string.sprig_hello_spanish);
        } else if (pick == 1) {
            return getString(R.string.sprig_next, stages.get(completed).getTitle());
        } else if (pick == 3) {
            return getString(R.string.sprig_today_minutes, (int) minutes);
        }
        return getString(R.string.sprig_cheer);
    }

    private void showSprigLine(String line) {
        binding.textSprigSays.setText(line);
        binding.textSprigSays.setAlpha(0f);
        binding.textSprigSays.animate().alpha(1f).setDuration(250);
    }

    // ---- Opening a stage ----

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

    // Explicit Intent. The "redo" extra tells the lesson page which mode to use.
    private void openStage(Stage stage, boolean redo) {
        Intent intent = new Intent(this, CourseActivity.class);
        intent.putExtra(EXTRA_LANGUAGE, language);
        intent.putExtra(CourseActivity.EXTRA_STAGE_ID, stage.getId());
        intent.putExtra(CourseActivity.EXTRA_STAGE_INDEX, stage.getOrderIndex());
        intent.putExtra(CourseActivity.EXTRA_STAGE_TITLE, stage.getTitle());
        intent.putExtra(CourseActivity.EXTRA_REDO, redo);
        startActivity(intent);
    }

    // The floating "Continue" button: jumps to the next unfinished stage
    private void continueLearning() {
        if (stages.isEmpty()) {
            return;
        }
        int completed = progressStore.getCompletedCount(language);
        if (completed >= stages.size()) {
            binding.textMessage.setText(R.string.fab_all_done);
            return;
        }
        openStage(stages.get(completed), false);
    }

    // ---- Cloud sync: the higher number wins ----

    private void syncWithCloud() {
        binding.textSyncStatus.setText(R.string.sync_syncing);

        cloudSync.downloadProgress(language, (reachedServer, cloudCompleted) -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            int local = progressStore.getCompletedCount(language);
            if (cloudCompleted > local) {
                progressStore.raiseCompletedTo(language, cloudCompleted);
                refreshProgress();
                binding.sprig.react();
                showSprigLine(getString(R.string.sprig_cloud_restored));
            } else if (local > cloudCompleted) {
                cloudSync.uploadProgress(language, local);
            }
            binding.textSyncStatus.setText(reachedServer ? R.string.sync_done : R.string.sync_offline);
        });
    }
}