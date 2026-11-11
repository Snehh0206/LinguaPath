package com.example.linguapath;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.linguapath.data.ProgressStore;
import com.example.linguapath.data.QuizHistoryRepository;
import com.example.linguapath.data.RewardRepository;
import com.example.linguapath.data.SavedWordsRepository;
import com.example.linguapath.data.ScenarioRepository;
import com.example.linguapath.data.TopicRepository;
import com.example.linguapath.databinding.ActivityScenarioBinding;
import com.example.linguapath.model.Topic;
import com.example.linguapath.speech.SpeechHelper;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// A guided situation: they say something, you pick the right reply. Connected to the topic's words.
public class ScenarioActivity extends AppCompatActivity {

    public static final String EXTRA_TOPIC_ID = "extra_topic_id";
    public static final String EXTRA_LANGUAGE = "extra_language";
    private static final int XP_REWARD = 5;

    private ActivityScenarioBinding binding;
    private SpeechHelper speech;
    private Topic topic;
    private String language = "Spanish";
    private List<ScenarioRepository.Step> steps = new ArrayList<>();
    private TextView[] optionViews;
    private int index = 0;
    private int correctCount = 0;
    private boolean answered = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityScenarioBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String languageExtra = getIntent().getStringExtra(EXTRA_LANGUAGE);
        if (languageExtra != null) {
            language = languageExtra;
        }
        String topicId = getIntent().getStringExtra(EXTRA_TOPIC_ID);

        optionViews = new TextView[]{binding.option0, binding.option1, binding.option2};
        for (int i = 0; i < optionViews.length; i++) {
            final int choice = i;
            optionViews[i].setOnClickListener(v -> answer(choice));
        }
        binding.buttonBack.setOnClickListener(v -> finish());
        binding.buttonDone.setOnClickListener(v -> finish());
        binding.buttonNext.setOnClickListener(v -> next());
        binding.buttonListen.setOnClickListener(v -> {
            if (speech != null && !steps.isEmpty()) {
                speech.speak(steps.get(index).say, language);
            }
        });
        binding.buttonSave.setOnClickListener(v -> savePhrases());
        binding.buttonAgain.setOnClickListener(v -> restart());

        TopicRepository.load(this, topics -> {
            if (gone()) {
                return;
            }
            for (Topic t : topics) {
                if (t.id.equals(topicId)) {
                    topic = t;
                }
            }
            if (topic == null) {
                binding.textMessage.setText(R.string.scenario_no_content);
                return;
            }
            binding.scenery.setSceneTheme(topic.theme);
            binding.textTitle.setText(topic.emoji + " " + topic.title);
            ScenarioRepository.load(this, topic.id, language, list -> {
                if (gone()) {
                    return;
                }
                steps = list;
                if (steps.isEmpty()) {
                    binding.textMessage.setText(R.string.scenario_no_content);
                } else {
                    showStep();
                }
            });
        });
    }

    private boolean gone() {
        return isFinishing() || isDestroyed();
    }

    // ---- Steps ----

    private void showStep() {
        ScenarioRepository.Step s = steps.get(index);
        answered = false;
        binding.layoutResult.setVisibility(View.GONE);
        binding.layoutStep.setVisibility(View.VISIBLE);
        binding.textStepCounter.setText(getString(R.string.scenario_step, index + 1, steps.size()));
        binding.textSay.setText(s.say);
        binding.textSayMeaning.setText(""); // the meaning appears after the answer
        binding.textAsk.setText(s.ask);
        binding.textFeedback.setText("");
        binding.buttonNext.setVisibility(View.GONE);

        for (int i = 0; i < optionViews.length; i++) {
            ScenarioRepository.Option o = s.options.get(i);
            optionViews[i].setText(o.rom.isEmpty() ? o.text : o.text + "\n" + o.rom);
            optionViews[i].setBackgroundResource(R.drawable.bg_word_card);
        }
    }

    private void answer(int choice) {
        if (answered) {
            return;
        }
        answered = true;
        ScenarioRepository.Step s = steps.get(index);
        boolean right = choice == s.correct;
        if (right) {
            correctCount++;
        }
        for (int i = 0; i < optionViews.length; i++) {
            ScenarioRepository.Option o = s.options.get(i);
            optionViews[i].setText((o.rom.isEmpty() ? o.text : o.text + "\n" + o.rom) + "\n(" + o.meaning + ")");
            if (i == s.correct) {
                optionViews[i].setBackgroundResource(R.drawable.bg_option_correct);
            } else if (i == choice) {
                optionViews[i].setBackgroundResource(R.drawable.bg_option_wrong);
            }
        }
        binding.textSayMeaning.setText(s.sayMeaning);
        binding.textFeedback.setText(right
                ? getString(R.string.scenario_correct)
                : getString(R.string.scenario_wrong, s.options.get(s.correct).text));
        binding.buttonNext.setText(index == steps.size() - 1 ? R.string.scenario_see_result : R.string.scenario_next);
        binding.buttonNext.setVisibility(View.VISIBLE);
    }

    private void next() {
        if (index < steps.size() - 1) {
            index++;
            showStep();
        } else {
            showResult();
        }
    }

    private void restart() {
        index = 0;
        correctCount = 0;
        showStep();
    }

    // ---- Result: XP once per situation, per language, per day ----

    private void showResult() {
        binding.layoutStep.setVisibility(View.GONE);
        binding.layoutResult.setVisibility(View.VISIBLE);
        binding.textResult.setText(getString(R.string.scenario_result, correctCount, steps.size()));
        binding.textSaveStatus.setText("");

        if (correctCount < 2) {
            binding.textResultXp.setText(R.string.scenario_xp_need);
            return;
        }
        SharedPreferences prefs = getSharedPreferences(
                "linguapath_scenarios_" + QuizHistoryRepository.currentUserId(), MODE_PRIVATE);
        String key = "done_" + LocalDate.now() + "_" + language + "_" + topic.id;
        if (prefs.getBoolean(key, false)) {
            binding.textResultXp.setText(R.string.scenario_xp_done);
        } else {
            prefs.edit().putBoolean(key, true).apply(); // marked first, so it is never paid twice
            new ProgressStore(this).addBonusXp(XP_REWARD);
            RewardRepository.syncMine(this);
            binding.textResultXp.setText(getString(R.string.scenario_xp, XP_REWARD));
        }
    }

    // The correct replies go into Saved Words, like the other saved words
    private void savePhrases() {
        final int[] left = {steps.size()};
        final int[] saved = {0};
        for (ScenarioRepository.Step s : steps) {
            ScenarioRepository.Option o = s.options.get(s.correct);
            SavedWordsRepository.saveTopicWord(this, language, o.text, o.meaning, topic.title, ok -> {
                if (ok) {
                    saved[0]++;
                }
                if (--left[0] == 0 && !gone()) {
                    binding.textSaveStatus.setText(getString(R.string.scenario_saved, saved[0]));
                }
            });
        }
    }

    // ---- Lifecycle (speech and scenery only run while visible) ----

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