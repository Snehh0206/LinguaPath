package com.example.linguapath;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.linguapath.adapter.TopicAdapter;
import com.example.linguapath.data.ReminderSettings;
import com.example.linguapath.data.TopicRepository;
import com.example.linguapath.databinding.ActivityTopicsBinding;
import com.example.linguapath.model.Topic;

import java.util.ArrayList;
import java.util.List;

public class TopicsActivity extends AppCompatActivity {

    private static final String STATE_LANGUAGE = "state_language";
    public static final String EXTRA_LANGUAGE = "extra_language";

    private ActivityTopicsBinding binding;
    private TopicAdapter adapter;
    private List<Topic> topics = new ArrayList<>();
    private String language = "Spanish";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTopicsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (savedInstanceState != null) {
            language = savedInstanceState.getString(STATE_LANGUAGE, "Spanish");
        } else {
            String fromIntent = getIntent().getStringExtra(EXTRA_LANGUAGE);
            String last = new ReminderSettings(this).getLastLanguage();
            if (fromIntent != null) {
                language = fromIntent;
            } else if (last != null) {
                language = last;
            }
        }
        binding.textTopicsSubtitle.setText(getString(R.string.topics_subtitle_language, language));
        binding.buttonBack.setOnClickListener(v -> finish());
        binding.chipKorean.setOnClickListener(v -> selectLanguage("Korean"));
        binding.chipSpanish.setOnClickListener(v -> selectLanguage("Spanish"));

        // A grid with 2 columns
        adapter = new TopicAdapter(this::openTopic);
        binding.recyclerTopics.setLayoutManager(new GridLayoutManager(this, 2));
        binding.recyclerTopics.setAdapter(adapter);

        showChips();
        TopicRepository.load(this, loaded -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            topics = loaded;
            adapter.setData(topics, language);
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_LANGUAGE, language);
    }

    @Override
    protected void onResume() {
        super.onResume();
        binding.scenery.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        binding.scenery.stop();
    }

    private void selectLanguage(String newLanguage) {
        language = newLanguage;
        showChips();
        adapter.setData(topics, language);
    }

    private void showChips() {
        boolean korean = "Korean".equals(language);
        binding.chipKorean.setBackgroundResource(korean ? R.drawable.bg_card_sage : R.drawable.bg_card_sky);
        binding.chipSpanish.setBackgroundResource(korean ? R.drawable.bg_card_sky : R.drawable.bg_card_sage);
    }

    private void openTopic(Topic topic) {
        Intent intent = new Intent(this, TopicWordsActivity.class);
        intent.putExtra(TopicWordsActivity.EXTRA_TOPIC_ID, topic.id);
        intent.putExtra(TopicWordsActivity.EXTRA_LANGUAGE, language);
        startActivity(intent);
    }
}