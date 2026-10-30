package com.example.linguapath;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.example.linguapath.data.ProgressStore;
import com.example.linguapath.databinding.ActivityCourseBinding;

public class CourseActivity extends AppCompatActivity {

    public static final String EXTRA_STAGE_INDEX = "extra_stage_index";
    public static final String EXTRA_STAGE_TITLE = "extra_stage_title";
    private static final String TAG = "CourseActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");

        ActivityCourseBinding binding = ActivityCourseBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String language = getIntent().getStringExtra(LanguageDashboardActivity.EXTRA_LANGUAGE);
        int stageIndex = getIntent().getIntExtra(EXTRA_STAGE_INDEX, 0);
        String stageTitle = getIntent().getStringExtra(EXTRA_STAGE_TITLE);

        if (language == null) {
            language = "Spanish";
        }
        binding.textCourseTitle.setText(stageTitle);

        // Back button: closes this screen and returns to the dashboard
        binding.buttonBack.setOnClickListener(v -> finish());

        ProgressStore progressStore = new ProgressStore(this);
        boolean alreadyDone = stageIndex < progressStore.getCompletedCount(language);

        if (alreadyDone) {
            binding.buttonComplete.setText(R.string.stage_already_done);
            binding.buttonComplete.setEnabled(false);
        } else {
            final String finalLanguage = language;
            binding.buttonComplete.setOnClickListener(v -> {
                progressStore.markStageCompleted(finalLanguage, stageIndex);
                finish();
            });
        }
    }
}