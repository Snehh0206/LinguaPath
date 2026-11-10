package com.example.linguapath;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.linguapath.adapter.WordAdapter;
import com.example.linguapath.data.ProgressStore;
import com.example.linguapath.data.WordRepository;
import com.example.linguapath.databinding.ActivityCourseBinding;
import com.example.linguapath.model.Word;
import com.example.linguapath.service.PlaybackController;
import com.example.linguapath.service.PlaybackState;
import com.example.linguapath.service.StudyTimerController;
import com.example.linguapath.service.SyncProgressService;
import com.example.linguapath.speech.SpeechHelper;
import com.example.linguapath.view.TopicTheme;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

// The page of ONE stage: all its word boxes, plus listening for this stage only.
public class CourseActivity extends AppCompatActivity {

    public static final String EXTRA_STAGE_ID = "extra_stage_id";
    public static final String EXTRA_STAGE_INDEX = "extra_stage_index";
    public static final String EXTRA_STAGE_TITLE = "extra_stage_title";
    public static final String EXTRA_REDO = "extra_redo";
    private static final String TAG = "CourseActivity";

    private ActivityCourseBinding binding;
    private WordAdapter adapter;
    private SpeechHelper speech;
    private ActivityResultLauncher<Intent> quizLauncher;
    private ActivityResultLauncher<String> notificationPermission;
    private final List<Word> loadedWords = new ArrayList<>();
    private final Random random = new Random();

    private String languageName = "Spanish";
    private int stageId;
    private int stageIndex;
    private String stageTitle = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");

        binding = ActivityCourseBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String languageExtra = getIntent().getStringExtra(LanguageDashboardActivity.EXTRA_LANGUAGE);
        languageName = (languageExtra != null) ? languageExtra : "Spanish";
        stageId = getIntent().getIntExtra(EXTRA_STAGE_ID, 0);
        stageIndex = getIntent().getIntExtra(EXTRA_STAGE_INDEX, 0);
        String titleExtra = getIntent().getStringExtra(EXTRA_STAGE_TITLE);
        stageTitle = (titleExtra == null) ? "" : titleExtra;
        boolean redo = getIntent().getBooleanExtra(EXTRA_REDO, false);
        final String language = languageName;

        binding.textCourseTitle.setText(stageTitle);
        TopicTheme theme = TopicTheme.forTitle(stageTitle);
        binding.textSticker.setText(theme.sticker);
        binding.headerBand.setBackgroundResource(theme.backgroundRes);

        binding.buttonBack.setOnClickListener(v -> finish());

        // The notification permission (Android 13+) is asked once, with a reason
        notificationPermission = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), granted -> { });

        // Floating "Surprise me": opens a random word from this stage
        binding.fabSurprise.setOnClickListener(v -> {
            if (!loadedWords.isEmpty()) {
                openWordDetail(loadedWords.get(random.nextInt(loadedWords.size())));
            }
        });

        // The word boxes. Tap a box to open its word page. Tap its 🔊 to hear it.
        adapter = new WordAdapter(this::openWordDetail);
        adapter.setSpeakListener(word -> {
            if (speech != null) {
                speech.speak(word.getWord(), languageName);
            }
        });
        binding.recyclerWords.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerWords.setAdapter(adapter);

        WordRepository.loadWords(this, stageId, words -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            adapter.setWords(words);
            loadedWords.clear();
            loadedWords.addAll(words);
            binding.recyclerWords.scheduleLayoutAnimation();
            int infoText = redo ? R.string.course_redo_info : R.string.course_word_count;
            binding.textCourseInfo.setText(getString(infoText, words.size()));
        });

        // Play all pronunciations / Audio lesson of THIS stage: a foreground service with its own notification
        binding.buttonPlayAll.setOnClickListener(v -> {
            PlaybackState.Snapshot s = PlaybackState.live().getValue();
            if (s != null && s.active) {
                PlaybackController.toggle(); // Pause or Play
            } else {
                PlaybackController.start(this, false, languageName, stageId, stageIndex, stageTitle);
            }
        });
        binding.buttonAudioLesson.setOnClickListener(v -> {
            PlaybackState.Snapshot s = PlaybackState.live().getValue();
            if (s != null && s.active) {
                PlaybackController.stop();
            } else {
                PlaybackController.start(this, true, languageName, stageId, stageIndex, stageTitle);
            }
        });
        PlaybackState.live().observe(this, this::showPlayback);

        ProgressStore progressStore = new ProgressStore(this);

        // The quiz decides whether the stage is finished
        quizLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        finish(); // passed: back to the path
                    }
                });

        binding.buttonComplete.setText(redo ? R.string.redo_finish : R.string.complete_stage);
        binding.buttonComplete.setOnClickListener(v ->
                startQuiz(language, stageId, stageIndex, stageTitle, redo));
    }

    @Override
    protected void onStart() {
        super.onStart();
        speech = new SpeechHelper(this);
        StudyTimerController.screenStarted();
        // Opening a stage starts the timer by itself
        StudyTimerController.begin(this, languageName, stageId, stageIndex, stageTitle);
        askNotificationPermissionOnce();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (speech != null) {
            speech.shutdown();
            speech = null;
        }
        StudyTimerController.screenStopped();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isFinishing()) {
            StudyTimerController.endSession(); // Back, or the stage is finished: the session ends
        }
    }
    // Explicit Intent with extras, and we wait for a result
    private void startQuiz(String language, int stageId, int stageIndex,
                           String stageTitle, boolean redo) {
        Intent intent = new Intent(this, QuizActivity.class);
        intent.putExtra(QuizActivity.EXTRA_LANGUAGE, language);
        intent.putExtra(QuizActivity.EXTRA_STAGE_ID, stageId);
        intent.putExtra(QuizActivity.EXTRA_STAGE_INDEX, stageIndex);
        intent.putExtra(QuizActivity.EXTRA_STAGE_TITLE, stageTitle);
        intent.putExtra(QuizActivity.EXTRA_REDO, redo);
        quizLauncher.launch(intent);
    }
    private void showPlayback(PlaybackState.Snapshot s) {
        if (s.active) {
            binding.buttonPlayAll.setText(s.playing ? R.string.playback_pause : R.string.playback_play);
            binding.buttonAudioLesson.setText(R.string.playback_stop);
            binding.textPlayback.setText(getString(R.string.playback_status,
                    Math.min(s.index + 1, Math.max(1, s.total)), s.total, s.currentWord));
            return;
        }
        binding.buttonPlayAll.setText(R.string.playback_play_all);
        binding.buttonAudioLesson.setText(R.string.playback_audio_lesson);
        if (s.errorRes != 0) {
            binding.textPlayback.setText(getString(s.errorRes, s.errorArg)); // for example a missing voice
        } else if (getSystemService(PowerManager.class).isPowerSaveMode()) {
            binding.textPlayback.setText(R.string.playback_battery_note);
        } else {
            binding.textPlayback.setText("");
        }
    }

    private void askNotificationPermissionOnce() {
        if (Build.VERSION.SDK_INT < 33
                || NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            return;
        }
        SharedPreferences prefs = getSharedPreferences("linguapath_timer", MODE_PRIVATE);
        if (prefs.getBoolean("asked", false)) {
            return;
        }
        prefs.edit().putBoolean("asked", true).apply();
        new AlertDialog.Builder(this)
                .setTitle(R.string.timer_permission_title)
                .setMessage(R.string.timer_permission_message)
                .setPositiveButton(android.R.string.ok, (dialog, which) ->
                        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS))
                .setNegativeButton(R.string.reminder_not_now, null)
                .show();
    }

    private void openWordDetail(Word word) {
        Intent intent = new Intent(this, WordDetailActivity.class);
        intent.putExtra(WordDetailActivity.EXTRA_WORD_ID, word.getId());
        intent.putExtra(WordDetailActivity.EXTRA_LANGUAGE, languageName);
        startActivity(intent);
    }
}