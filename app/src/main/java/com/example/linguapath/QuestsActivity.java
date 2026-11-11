package com.example.linguapath;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.linguapath.data.QuestRepository;
import com.example.linguapath.databinding.ActivityQuestsBinding;
import com.example.linguapath.view.Motion;

public class QuestsActivity extends AppCompatActivity {

    private ActivityQuestsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityQuestsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.buttonBack.setOnClickListener(v -> finish());
        // The leaderboard is its own page, not part of this one
        binding.buttonLeaderboard.setOnClickListener(v ->
                startActivity(new Intent(this, LeaderboardActivity.class)));
    }

    @Override
    protected void onStart() {
        super.onStart();
        show(); // right away from what is on the phone
        QuestRepository.restoreAndRefresh(this, paid -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            for (QuestRepository.Quest q : paid) {
                Toast.makeText(this, getString(R.string.quest_toast, q.rewardXp), Toast.LENGTH_SHORT).show();
            }
            show();
        });
    }

    private void show() {
        TextView[] titles = {binding.textQuestTitle0, binding.textQuestTitle1};
        ProgressBar[] bars = {binding.progressQuest0, binding.progressQuest1};
        TextView[] progress = {binding.textQuestProgress0, binding.textQuestProgress1};
        TextView[] rewards = {binding.textQuestReward0, binding.textQuestReward1};

        for (int i = 0; i < QuestRepository.DAILY.length; i++) {
            QuestRepository.Quest q = QuestRepository.DAILY[i];
            int done = Math.min(QuestRepository.progress(this, q), q.target);

            titles[i].setText(q.titleRes);
            Motion.animateProgress(bars[i], done, q.target);
            progress[i].setText(getString(R.string.quest_progress, done, q.target));
            rewards[i].setText(QuestRepository.isClaimed(this, q)
                    ? getString(R.string.quest_earned, q.rewardXp)
                    : getString(R.string.quest_reward, q.rewardXp));
        }
    }
}