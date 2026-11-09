package com.example.linguapath;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
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
import com.example.linguapath.service.StudyTimerController;
import com.example.linguapath.service.SyncProgressService;
import com.example.linguapath.speech.SpeechHelper;
import com.example.linguapath.view.TopicTheme;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CourseActivity extends AppCompatActivity {

    public static final String EXTRA_STAGE_ID = "extra_stage_id";
    public static final String EXTRA_STAGE_INDEX = "extra_stage_index";
    public static final String EXTRA_STAGE_TITLE = "extra_stage_title";
    public static final String EXTRA_REDO = "extra_redo";
    private static final String TAG = "CourseActivity";

    private ActivityCourseBinding binding;
    private WordAdapter adapter;
    private SpeechHelper speech;
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

        // The timer notification needs a permission on Android 13+. It is asked once, with a reason.
        notificationPermission = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), granted -> { });

        // Floating "Surprise me": opens a random word from this stage
        binding.fabSurprise.setOnClickListener(v -> {
            if (!loadedWords.isEmpty()) {
                openWordDetail(loadedWords.get(random.nextInt(loadedWords.size())));
            }
        });

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

        ProgressStore progressStore = new ProgressStore(this);

        if (redo) {
            // Redo: counts the practice, never changes progress
            binding.buttonComplete.setText(R.string.redo_finish);
            binding.buttonComplete.setOnClickListener(v -> {
                progressStore.addPractice(language, stageIndex);
                StudyTimerController.endSession(); // saves this stage's time and stops the service
                finish();
            });
        } else {
            binding.buttonComplete.setOnClickListener(v -> {
                progressStore.markStageCompleted(language, stageIndex);
                StudyTimerController.endSession();
                // A background service uploads the progress to Firebase, then stops itself
                startService(new Intent(this, SyncProgressService.class)
                        .putExtra(SyncProgressService.EXTRA_LANGUAGE, language));
                finish();
            });
        }
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