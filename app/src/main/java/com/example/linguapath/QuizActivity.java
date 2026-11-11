package com.example.linguapath;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.linguapath.data.CloudSyncRepository;
import com.example.linguapath.data.ProgressStore;
import com.example.linguapath.databinding.ActivityQuizBinding;
import com.example.linguapath.quiz.QuizQuestion;
import com.example.linguapath.quiz.QuizViewModel;
import com.example.linguapath.view.Motion;
import com.example.linguapath.view.TouchBounce;
import com.example.linguapath.data.QuizAttempt;
import com.example.linguapath.data.QuizHistoryRepository;
import com.example.linguapath.data.WordStatsRepository;
import com.example.linguapath.speech.SpeechHelper;
import com.example.linguapath.data.QuestRepository;

import java.util.List;

public class QuizActivity extends AppCompatActivity {

    public static final String EXTRA_LANGUAGE = "extra_language";
    public static final String EXTRA_STAGE_ID = "extra_stage_id";
    public static final String EXTRA_STAGE_INDEX = "extra_stage_index";
    public static final String EXTRA_STAGE_TITLE = "extra_stage_title";
    public static final String EXTRA_REDO = "extra_redo";
    public static final String EXTRA_REVISION = "extra_revision";
    public static final String EXTRA_TOPIC_ID = "extra_topic_id";
    public static final String EXTRA_TOPIC_TITLE = "extra_topic_title";

    private static final String TAG = "QuizActivity";
    private static final int PASS_PERCENT = 70;
    private static final int PERFECT_BONUS = 5;

    private ActivityQuizBinding binding;
    private QuizViewModel viewModel;
    private TextView[] optionViews;
    private SpeechHelper speech;

    private String language;
    private int stageId;
    private int stageIndex;
    private String stageTitle;
    private boolean redo;
    private boolean revision;
    private String topicId;
    private String topicTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");

        binding = ActivityQuizBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String languageExtra = getIntent().getStringExtra(EXTRA_LANGUAGE);
        language = (languageExtra != null) ? languageExtra : "Spanish";
        stageId = getIntent().getIntExtra(EXTRA_STAGE_ID, 0);
        stageIndex = getIntent().getIntExtra(EXTRA_STAGE_INDEX, 0);
        stageTitle = getIntent().getStringExtra(EXTRA_STAGE_TITLE);
        redo = getIntent().getBooleanExtra(EXTRA_REDO, false);
        revision = getIntent().getBooleanExtra(EXTRA_REVISION, false);
        topicId = getIntent().getStringExtra(EXTRA_TOPIC_ID);
        topicTitle = getIntent().getStringExtra(EXTRA_TOPIC_TITLE);

        // The ViewModel is the same object after a rotation
        viewModel = new ViewModelProvider(this).get(QuizViewModel.class);

        optionViews = new TextView[]{binding.option0, binding.option1, binding.option2, binding.option3};
        for (int i = 0; i < optionViews.length; i++) {
            final int choice = i;
            optionViews[i].setOnClickListener(v -> onOptionPicked(choice));
            TouchBounce.attach(optionViews[i]);
        }

        binding.buttonNext.setOnClickListener(v -> next());
        binding.buttonListen.setOnClickListener(v -> {
            if (speech != null) {
                speech.speak(current().speakText, language);
            }
        });
        binding.buttonShowWord.setOnClickListener(v -> {
            viewModel.revealed = true;
            showQuestion(false);
        });
        binding.buttonClose.setOnClickListener(v -> leave());

        // Back button and gesture
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                leave();
            }
        });

        viewModel.getQuestions().observe(this, questions -> render());
        viewModel.getResultReady().observe(this, ready -> render());
        viewModel.load(this, stageId, language, revision, topicId);
    }

    // Draws the right screen for the current state: loading, a question, or the result
    private void render() {
        List<QuizQuestion> questions = viewModel.getQuestions().getValue();
        if (questions == null) {
            showLoading(R.string.quiz_loading);
            return;
        }
        if (questions.isEmpty()) {
            showLoading(R.string.quiz_not_enough_words);
            return;
        }
        if (viewModel.finished) {
            if (Boolean.TRUE.equals(viewModel.getResultReady().getValue())) {
                showResult(questions.size());
            } else {
                showLoading(R.string.quiz_loading);
            }
            return;
        }
        showQuestion(false);
    }

    private void showLoading(int messageResId) {
        binding.textLoading.setText(messageResId);
        binding.textLoading.setVisibility(View.VISIBLE);
        binding.groupQuestion.setVisibility(View.GONE);
        binding.groupResult.setVisibility(View.GONE);
    }

    // ---- Questions ----

    private QuizQuestion current() {
        return viewModel.getQuestions().getValue().get(viewModel.index);
    }

    private void showQuestion(boolean animate) {
        List<QuizQuestion> questions = viewModel.getQuestions().getValue();
        QuizQuestion q = questions.get(viewModel.index);
        int total = questions.size();

        binding.textLoading.setVisibility(View.GONE);
        binding.groupResult.setVisibility(View.GONE);
        binding.groupQuestion.setVisibility(View.VISIBLE);

        binding.textProgress.setText(getString(R.string.quiz_progress, viewModel.index + 1, total));
        Motion.animateProgress(binding.progressQuiz, viewModel.index, total);

        boolean listen = q.type == QuizQuestion.TYPE_LISTEN;
        int typeText = (q.type == QuizQuestion.TYPE_WORD) ? R.string.quiz_type_word
                : listen ? R.string.quiz_type_listen : R.string.quiz_type_meaning;
        binding.textQuestionType.setText(typeText);

        // Listening: the word stays hidden until answered, or until "Show the word" is tapped
        boolean hideWord = listen && !viewModel.revealed && viewModel.selected == -1;
        binding.buttonListen.setVisibility(listen ? View.VISIBLE : View.GONE);
        binding.buttonShowWord.setVisibility(hideWord ? View.VISIBLE : View.GONE);

        binding.textPrompt.setText(q.prompt);
        binding.textPrompt.setVisibility(hideWord ? View.GONE : View.VISIBLE);
        binding.textPromptSub.setText(q.promptSub);
        binding.textPromptSub.setVisibility(
                (q.promptSub.isEmpty() || hideWord) ? View.GONE : View.VISIBLE);

        for (int i = 0; i < optionViews.length; i++) {
            if (i < q.options.size()) {
                optionViews[i].setVisibility(View.VISIBLE);
                optionViews[i].setText(q.options.get(i));
                optionViews[i].setBackgroundResource(R.drawable.bg_word_card);
                optionViews[i].setEnabled(true);
            } else {
                optionViews[i].setVisibility(View.GONE);
            }
        }
        binding.textFeedback.setText("");
        binding.buttonNext.setVisibility(View.GONE);

        if (viewModel.selected != -1) {
            applyFeedback(q); // for example after a rotation: show the answer again
        } else if (animate) {
            Motion.fadeSlideIn(binding.groupQuestion, 0);
        }
    }

    private void onOptionPicked(int choice) {
        if (viewModel.selected != -1) {
            return; // already answered
        }
        QuizQuestion q = current();
        viewModel.selected = choice;
        if (choice == q.correctIndex) {
            viewModel.score++;
            viewModel.correctIds.add(q.wordId);
        } else {
            viewModel.wrongIds.add(q.wordId); // remembered for revision
        }
        applyFeedback(q);
    }

    // Green for the right answer, red for a wrong pick
    private void applyFeedback(QuizQuestion q) {
        binding.textPrompt.setVisibility(View.VISIBLE);
        binding.buttonShowWord.setVisibility(View.GONE);
        binding.textPromptSub.setVisibility(q.promptSub.isEmpty() ? View.GONE : View.VISIBLE);
        boolean correct = viewModel.selected == q.correctIndex;

        for (TextView option : optionViews) {
            option.setEnabled(false);
        }
        optionViews[q.correctIndex].setBackgroundResource(R.drawable.bg_option_correct);
        if (!correct) {
            optionViews[viewModel.selected].setBackgroundResource(R.drawable.bg_option_wrong);
        }

        binding.textFeedback.setText(correct
                ? getString(R.string.quiz_correct)
                : getString(R.string.quiz_wrong, q.options.get(q.correctIndex)));

        boolean last = viewModel.index + 1 >= viewModel.getQuestions().getValue().size();
        binding.buttonNext.setText(last ? R.string.quiz_see_results : R.string.quiz_next);
        binding.buttonNext.setVisibility(View.VISIBLE);
    }

    private void next() {
        int total = viewModel.getQuestions().getValue().size();
        if (viewModel.index + 1 >= total) {
            finishQuiz(total);
            return;
        }
        viewModel.index++;
        viewModel.revealed = false;
        viewModel.selected = -1;
        showQuestion(true);
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

    // ---- Finishing ----

    private void finishQuiz(int total) {
        viewModel.finished = true;
        boolean topicQuiz = (topicId != null);

        // Save only once, even if the phone is rotated on the result screen
        if (!viewModel.resultSaved) {
            viewModel.resultSaved = true;

            int percent = Math.round(100f * viewModel.score / total);
            boolean passed = percent >= PASS_PERCENT;
            viewModel.percent = percent;
            viewModel.passed = passed;

            ProgressStore store = new ProgressStore(this);
            int xp = 0;
            if (revision || topicQuiz) {
                // Revision and topic quizzes never change stage progress or XP
            } else if (redo) {
                store.addPractice(language, stageIndex); // practice: no progress, no XP
            } else if (passed) {
                boolean firstTime = stageIndex == store.getCompletedCount(language);
                store.markStageCompleted(language, stageIndex);
                if (firstTime) {
                    xp = ProgressStore.XP_PER_STAGE;
                    if (percent == 100) {
                        store.addBonusXp(PERFECT_BONUS);
                        xp += PERFECT_BONUS;
                        viewModel.perfectBonus = true;
                    }
                }
                new CloudSyncRepository().uploadProgress(language, store.getCompletedCount(language));
            }
            viewModel.xpGained = xp;
            QuestRepository.onQuizFinished(this); // counts the quiz for the daily quest and syncs XP

            // Topic words are not lesson words, so they must not enter the word scores
            if (!topicQuiz) {
                WordStatsRepository.recordAnswers(getApplicationContext(), language,
                        new java.util.ArrayList<>(viewModel.correctIds),
                        new java.util.ArrayList<>(viewModel.wrongIds));
            }

            // stageIndex: -1 = revision quiz, -2 = topic quiz
            int savedIndex = topicQuiz ? -2 : revision ? -1 : stageIndex;
            String savedTitle = topicQuiz ? (topicTitle == null ? "" : topicTitle)
                    : revision ? getString(R.string.revision_label)
                    : (stageTitle == null ? "" : stageTitle);

            final QuizViewModel vm = viewModel; // not the Activity, so a rotation can't leak it
            QuizAttempt attempt = new QuizAttempt(
                    QuizHistoryRepository.currentUserId(), language, savedIndex, savedTitle,
                    viewModel.score, total, percent, passed, redo || revision || topicQuiz,
                    System.currentTimeMillis());
            QuizHistoryRepository.record(getApplicationContext(), attempt, newBest -> vm.markReady(newBest));
        }
        render();
    }

    private void showResult(int total) {
        binding.textLoading.setVisibility(View.GONE);
        binding.groupQuestion.setVisibility(View.GONE);
        binding.groupResult.setVisibility(View.VISIBLE);
        Motion.animateProgress(binding.progressQuiz, total, total);
        binding.textProgress.setText("");

        binding.textResultScore.setText(
                getString(R.string.quiz_score, viewModel.score, total, viewModel.percent));

        if (viewModel.passed) {
            binding.textResultEmoji.setText("🎉");
            binding.textResultTitle.setText(R.string.quiz_passed_title);
            StringBuilder message = new StringBuilder();
            if (redo || revision || topicId != null) {
                message.append(getString(R.string.quiz_no_xp_redo));
            } else {
                message.append(getString(R.string.quiz_xp_gained, viewModel.xpGained));
                if (viewModel.perfectBonus) {
                    message.append("\n").append(getString(R.string.quiz_perfect_bonus, PERFECT_BONUS));
                }
            }
            if (viewModel.newBest) {
                message.append("\n").append(getString(R.string.quiz_new_best));
            }
            binding.textResultMessage.setText(message.toString());

            binding.buttonResultPrimary.setText(R.string.quiz_back_to_path);
            binding.buttonResultPrimary.setOnClickListener(v -> {
                setResult(RESULT_OK); // the course screen closes too, back to the path
                finish();
            });

            // The confetti plays once, not again after a rotation
            if (!viewModel.confettiShown) {
                viewModel.confettiShown = true;
                binding.confetti.post(() -> binding.confetti.burst());
            }
        } else {
            binding.textResultEmoji.setText("🌱");
            binding.textResultTitle.setText(R.string.quiz_failed_title);
            binding.textResultMessage.setText(getString(R.string.quiz_pass_hint, PASS_PERCENT));

            binding.buttonResultPrimary.setText(R.string.quiz_try_again);
            binding.buttonResultPrimary.setOnClickListener(v ->
                    viewModel.restart(getApplicationContext(), stageId, language, revision, topicId));
        }

        binding.buttonResultSecondary.setOnClickListener(v -> {
            setResult(RESULT_CANCELED); // back to the word list
            finish();
        });
    }

    // ---- Leaving ----

    private void leave() {
        if (viewModel.finished) {
            setResult(RESULT_CANCELED);
            finish();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.quiz_leave_title)
                .setMessage(R.string.quiz_leave_message)
                .setPositiveButton(R.string.quiz_leave_yes, (dialog, which) -> {
                    setResult(RESULT_CANCELED);
                    finish();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }
}