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

public class CourseActivity extends AppCompatActivity {

    public static final String EXTRA_STAGE_ID = "extra_stage_id";
    public static final String EXTRA_STAGE_INDEX = "extra_stage_index";
    public static final String EXTRA_STAGE_TITLE = "extra_stage_title";
    public static final String EXTRA_REDO = "extra_redo";
    private static final String TAG = "CourseActivity";

    private ActivityCourseBinding binding;
    private WordAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");

        binding = ActivityCourseBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String languageExtra = getIntent().getStringExtra(LanguageDashboardActivity.EXTRA_LANGUAGE);
        final String language = (languageExtra != null) ? languageExtra : "Spanish";
        int stageId = getIntent().getIntExtra(EXTRA_STAGE_ID, 0);
        int stageIndex = getIntent().getIntExtra(EXTRA_STAGE_INDEX, 0);
        String stageTitle = getIntent().getStringExtra(EXTRA_STAGE_TITLE);
        boolean redo = getIntent().getBooleanExtra(EXTRA_REDO, false);

        binding.textCourseTitle.setText(stageTitle);
        binding.buttonBack.setOnClickListener(v -> finish());

        adapter = new WordAdapter(this::openWordDetail);
        binding.recyclerWords.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerWords.setAdapter(adapter);

        WordRepository.loadWords(this, stageId, words -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            adapter.setWords(words);
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
                finish();
            });
        }
    }

    private void openWordDetail(Word word) {
        Intent intent = new Intent(this, WordDetailActivity.class);
        intent.putExtra(WordDetailActivity.EXTRA_WORD_ID, word.getId());
        startActivity(intent);
    }
}