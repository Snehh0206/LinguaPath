package com.example.linguapath;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.linguapath.adapter.WordAdapter;
import com.example.linguapath.data.ProgressStore;
import com.example.linguapath.data.WordRepository;
import com.example.linguapath.databinding.ActivityCourseBinding;
import com.example.linguapath.model.Word;
import com.example.linguapath.view.TopicTheme;
import com.example.linguapath.data.CloudSyncRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import com.example.linguapath.service.StudySessionClient;
import com.example.linguapath.speech.SpeechHelper;

public class CourseActivity extends AppCompatActivity {

    public static final String EXTRA_STAGE_ID = "extra_stage_id";
    public static final String EXTRA_STAGE_INDEX = "extra_stage_index";
    public static final String EXTRA_STAGE_TITLE = "extra_stage_title";
    public static final String EXTRA_REDO = "extra_redo";
    private static final String TAG = "CourseActivity";
    private SpeechHelper speech;
    private ActivityCourseBinding binding;
    private WordAdapter adapter;
    private String languageName = "Spanish";
    private StudySessionClient studyClient;

    private final List<Word> loadedWords = new ArrayList<>();
    private final Random random = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");

        binding = ActivityCourseBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String languageExtra = getIntent().getStringExtra(LanguageDashboardActivity.EXTRA_LANGUAGE);
        final String language = (languageExtra != null) ? languageExtra : "Spanish";
        languageName = language;
        int stageId = getIntent().getIntExtra(EXTRA_STAGE_ID, 0);
        int stageIndex = getIntent().getIntExtra(EXTRA_STAGE_INDEX, 0);
        String stageTitle = getIntent().getStringExtra(EXTRA_STAGE_TITLE);
        boolean redo = getIntent().getBooleanExtra(EXTRA_REDO, false);

        binding.textCourseTitle.setText(stageTitle);
        TopicTheme theme = TopicTheme.forTitle(stageTitle);
        binding.textSticker.setText(theme.sticker);
        binding.headerBand.setBackgroundResource(theme.backgroundRes);

        binding.buttonBack.setOnClickListener(v -> finish());
        // Same study timer as the dashboard: one service, many screens
        studyClient = new StudySessionClient(this, (elapsedMs, running) ->
                binding.textStudyTime.setText(getString(
                        running ? R.string.timer_running : R.string.timer_paused,
                        StudySessionClient.formatTime(elapsedMs))));
        binding.textStudyTime.setOnClickListener(v -> studyClient.toggle());
        // Floating "Surprise me": opens a random word from this stage
        binding.fabSurprise.setOnClickListener(v -> {
            if (!loadedWords.isEmpty()) {
                openWordDetail(loadedWords.get(random.nextInt(loadedWords.size())));
            }
        });

        adapter = new WordAdapter(this::openWordDetail);
        binding.recyclerWords.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerWords.setAdapter(adapter);
        // The 🔊 on each card speaks that word in the stage's language
        adapter.setSpeakListener(word -> {
            if (speech != null) {
                speech.speak(word.getWord(), languageName);
            }
        });

        WordRepository.loadWords(this, stageId, words -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            loadedWords.clear();
            loadedWords.addAll(words);

            adapter.setWords(words);
            binding.recyclerWords.scheduleLayoutAnimation();

            int infoText = redo ? R.string.course_redo_info : R.string.course_word_count;
            binding.textCourseInfo.setText(getString(infoText, words.size()));
        });

        ProgressStore progressStore = new ProgressStore(this);

        if (redo) {
            // Redo: counts the redo, never changes progress
            binding.buttonComplete.setText(R.string.redo_finish);
            binding.buttonComplete.setOnClickListener(v -> {
                progressStore.addPractice(language, stageIndex);
                finish();
            });
        } else {
            binding.buttonComplete.setOnClickListener(v -> {
                progressStore.markStageCompleted(language, stageIndex);
                // Back up the new progress to the cloud
                new CloudSyncRepository().uploadProgress(
                        language, progressStore.getCompletedCount(language));
                finish();
            });

        }
    }
    @Override
    protected void onStart() {
        super.onStart();
        studyClient.bind();
        speech = new SpeechHelper(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        studyClient.unbind();
        if (speech != null) {
            speech.shutdown();
            speech = null;
        }
    }
    private void openWordDetail(Word word) {
        Intent intent = new Intent(this, WordDetailActivity.class);
        intent.putExtra(WordDetailActivity.EXTRA_WORD_ID, word.getId());
        intent.putExtra(WordDetailActivity.EXTRA_LANGUAGE, languageName);
        startActivity(intent);
    }
}