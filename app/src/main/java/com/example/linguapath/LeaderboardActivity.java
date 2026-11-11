package com.example.linguapath;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.linguapath.adapter.LeaderAdapter;
import com.example.linguapath.data.ChatRepository;
import com.example.linguapath.data.ChatScoreRepository;
import com.example.linguapath.data.FriendRepository;
import com.example.linguapath.databinding.ActivityLeaderboardBinding;

import java.util.List;

// Friends leaderboard: me + my friends, ranked by XP or by chat points
public class LeaderboardActivity extends AppCompatActivity {

    private ActivityLeaderboardBinding binding;
    private LeaderAdapter adapter;
    private boolean byXp = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLeaderboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.buttonBack.setOnClickListener(v -> finish());
        binding.tabXp.setOnClickListener(v -> {
            byXp = true;
            load();
        });
        binding.tabPoints.setOnClickListener(v -> {
            byXp = false;
            load();
        });

        adapter = new LeaderAdapter(ChatRepository.currentUid());
        binding.recyclerLeaders.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerLeaders.setAdapter(adapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        load();
    }

    private void load() {
        binding.tabXp.setBackgroundResource(byXp ? R.drawable.bg_card_butter : R.drawable.bg_word_card);
        binding.tabPoints.setBackgroundResource(byXp ? R.drawable.bg_word_card : R.drawable.bg_card_butter);
        binding.textLeaderStatus.setText(R.string.leaderboard_loading);
        binding.textLeaderStatus.setVisibility(View.VISIBLE);

        FriendRepository.loadLeaderboard(byXp, new ChatScoreRepository.LeaderboardCallback() {
            @Override
            public void onLoaded(List<ChatScoreRepository.Entry> entries) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                adapter.setMode(byXp);
                adapter.setItems(entries);
                binding.textLeaderStatus.setText(R.string.leaderboard_empty);
                // only me in the list = no friends yet, so show the hint under my row
                binding.textLeaderStatus.setVisibility(entries.size() <= 1 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onFailed() {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                binding.textLeaderStatus.setText(R.string.leaderboard_failed);
                binding.textLeaderStatus.setVisibility(View.VISIBLE);
            }
        });
    }
}