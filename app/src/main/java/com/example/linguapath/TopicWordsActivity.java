package com.example.linguapath;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.linguapath.adapter.TopicWordAdapter;
import com.example.linguapath.data.SavedWordsRepository;
import com.example.linguapath.data.TopicRepository;
import com.example.linguapath.databinding.ActivityTopicWordsBinding;
import com.example.linguapath.model.Topic;
import com.example.linguapath.model.TopicWord;
import com.example.linguapath.speech.SpeechHelper;
import android.content.Intent;
import com.example.linguapath.data.ScenarioRepository;

import java.util.List;

public class TopicWordsActivity extends AppCompatActivity {

    public static final String EXTRA_TOPIC_ID = "extra_topic_id";
    public static final String EXTRA_LANGUAGE = "extra_language";

    private ActivityTopicWordsBinding binding;
    private TopicWordAdapter adapter;
    private SpeechHelper speech;
    private Topic topic;
    private String language = "Spanish";
    private boolean learnMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTopicWordsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String languageExtra = getIntent().getStringExtra(EXTRA_LANGUAGE);
        if (languageExtra != null) {
            language = languageExtra;
        }
        String topicId = getIntent().getStringExtra(EXTRA_TOPIC_ID);

        binding.buttonBack.setOnClickListener(v -> finish());

        adapter = new TopicWordAdapter();
        adapter.setSpeakListener(word -> {
            if (speech != null) {
                speech.speak(word.word, language);
            }
        });
        adapter.setSaveListener(this::saveWord);
        binding.recyclerTopicWords.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerTopicWords.setAdapter(adapter);

        TopicRepository.load(this, topics -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            for (Topic t : topics) {
                if (t.id.equals(topicId)) {
                    bindTopic(t);
                    return;
                }
            }
            binding.textMessage.setText(R.string.word_not_found);
        });
    }

    private void bindTopic(Topic t) {
        topic = t;
        binding.scenery.setSceneTheme(t.theme);
        binding.textBigEmoji.setText(t.emoji);
        binding.textTopicTitle.setText(t.title);

        List<TopicWord> words = t.getWords(language);
        binding.textTopicInfo.setText(getString(R.string.topic_info, words.size(), language));
        adapter.setWords(words);
        // Your own painted background wins, if the file exists (see the notes below)
        int paintedRes = getResources().getIdentifier("topic_bg_" + t.id, "drawable", getPackageName());
        if (paintedRes != 0) {
            binding.imageTopicBackground.setImageResource(paintedRes);
            binding.imageTopicBackground.setVisibility(View.VISIBLE);
            binding.scenery.setVisibility(View.GONE);
        }

        // Learn mode and the quiz
        binding.rowTools.setVisibility(View.VISIBLE);
        showLearnButton();
        binding.buttonQuiz.setText(R.string.topic_quiz_button);
        binding.buttonLearn.setOnClickListener(v -> {
            learnMode = !learnMode;
            adapter.setLearnMode(learnMode);
            showLearnButton();
        });
        binding.buttonQuiz.setOnClickListener(v -> startTopicQuiz());
        // Show the situation button only when this topic has a situation for this language
        ScenarioRepository.load(this, t.id, language, list -> {
            if (isFinishing() || isDestroyed() || list.isEmpty()) {
                return;
            }
            binding.buttonScenario.setVisibility(View.VISIBLE);
            binding.buttonScenario.setOnClickListener(v -> {
                Intent intent = new Intent(this, ScenarioActivity.class);
                intent.putExtra(ScenarioActivity.EXTRA_TOPIC_ID, t.id);
                intent.putExtra(ScenarioActivity.EXTRA_LANGUAGE, language);
                startActivity(intent);
            });
        });
    }

    // Saved through the Content Provider, like the other saved words
    private void saveWord(TopicWord word) {
        if (topic == null) {
            return;
        }
        SavedWordsRepository.saveTopicWord(this, language, word.word, word.meaning, topic.title, saved -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            binding.textMessage.setText(saved ? R.string.topic_saved : R.string.topic_already_saved);
        });
    }
    private void showLearnButton() {
        binding.buttonLearn.setText(learnMode ? R.string.learn_mode_on : R.string.learn_mode_off);
    }

    // Explicit Intent: a quiz made only from this topic's words
    private void startTopicQuiz() {
        if (topic == null) {
            return;
        }
        Intent intent = new Intent(this, QuizActivity.class);
        intent.putExtra(QuizActivity.EXTRA_LANGUAGE, language);
        intent.putExtra(QuizActivity.EXTRA_TOPIC_ID, topic.id);
        intent.putExtra(QuizActivity.EXTRA_TOPIC_TITLE, topic.title);
        startActivity(intent);
    }

    @Override
    protected void onStart() {
        super.onStart();
        speech = new SpeechHelper(this);
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
        binding.scenery.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        binding.scenery.stop();
    }
}