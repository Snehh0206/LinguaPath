package com.example.linguapath;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.linguapath.data.AuthRepository;
import com.example.linguapath.data.ChatRepository;
import com.example.linguapath.data.ChatScoreRepository;
import com.example.linguapath.data.ProgressStore;
import com.example.linguapath.data.QuizHistoryRepository;
import com.example.linguapath.data.RewardRepository;
import com.example.linguapath.databinding.ActivityAccountBinding;
import com.example.linguapath.view.Motion;

import java.util.Map;

// Profile, XP, reward points, level and progress. Separate from the learning screens.
public class AccountActivity extends AppCompatActivity {

    private ActivityAccountBinding binding;
    private ChatRepository.Registration scoreRegistration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAccountBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String email = new AuthRepository().getEmail();
        binding.textEmail.setText(email == null ? "" : email);
        binding.buttonBack.setOnClickListener(v -> finish());
        binding.buttonQuests.setOnClickListener(v ->
                startActivity(new Intent(this, QuestsActivity.class)));
    }

    @Override
    protected void onStart() {
        super.onStart();
        ProgressStore store = new ProgressStore(this);
        int xp = store.getXp();
        int level = RewardRepository.levelForXp(xp);
        int into = RewardRepository.xpIntoLevel(xp);

        binding.textLevel.setText(getString(R.string.account_level, level));
        Motion.animateProgress(binding.progressLevel, into, RewardRepository.XP_PER_LEVEL);
        binding.textLevelHint.setText(getString(R.string.account_to_next,
                into, RewardRepository.XP_PER_LEVEL, level + 1));
        binding.textXp.setText(getString(R.string.account_xp, xp));
        binding.textStages.setText(getString(R.string.account_stages, store.getTotalCompleted()));
        showLanguage("Korean", binding.textLangKorean);
        showLanguage("Spanish", binding.textLangSpanish);


        showLanguage("Bangla", binding.textLangBangla);
        showLanguage("Japanese", binding.textLangJapanese);
        showLanguage("French", binding.textLangFrench);

        RewardRepository.syncMine(this);

        ChatRepository.loadNickname((name, failed) -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            boolean has = name != null && !name.isEmpty();
            binding.textName.setText(has ? name : getString(R.string.account_no_name));
            binding.textAvatar.setText(has ? name.substring(0, 1).toUpperCase() : "?");
        });

        // Reward points (chat), updated live
        scoreRegistration = ChatScoreRepository.listenMyScore(points ->
                binding.textPoints.setText(getString(R.string.account_points, points)));
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (scoreRegistration != null) {
            scoreRegistration.remove();
            scoreRegistration = null;
        }
    }

    private void showLanguage(String language, TextView view) {
        int stages = new ProgressStore(this).getCompletedCount(language);
        QuizHistoryRepository.loadBestScores(this, language, best -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            int sum = 0;
            int count = 0;
            for (Map.Entry<Integer, Integer> entry : best.entrySet()) {
                if (entry.getKey() >= 0) { // stage quizzes only, not revision or topic quizzes
                    sum += entry.getValue();
                    count++;
                }
            }
            if (stages == 0 && count == 0) {
                view.setText(getString(R.string.account_lang_none, language));
            } else {
                view.setText(getString(R.string.account_lang_row,
                        language, stages, count == 0 ? 0 : sum / count));
            }
        });
    }
}