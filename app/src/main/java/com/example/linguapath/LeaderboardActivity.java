package com.example.linguapath;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.linguapath.adapter.LeaderAdapter;
import com.example.linguapath.data.ChatRepository;
import com.example.linguapath.data.ChatScoreRepository;
import com.example.linguapath.databinding.ActivityLeaderboardBinding;

import java.util.List;

public class LeaderboardActivity extends AppCompatActivity {

    private ActivityLeaderboardBinding binding;
    private LeaderAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLeaderboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.buttonBack.setOnClickListener(v -> finish());

        adapter = new LeaderAdapter(ChatRepository.currentUid());
        binding.recyclerLeaders.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerLeaders.setAdapter(adapter);
    }

    // Refresh every time the screen is shown
    @Override
    protected void onStart() {
        super.onStart();
        binding.textLeaderStatus.setText(R.string.leaderboard_loading);
        binding.textLeaderStatus.setVisibility(View.VISIBLE);

        ChatScoreRepository.loadLeaderboard(new ChatScoreRepository.LeaderboardCallback() {
            @Override
            public void onLoaded(List<ChatScoreRepository.Entry> entries) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                adapter.setItems(entries);
                binding.textLeaderStatus.setText(R.string.leaderboard_empty);
                binding.textLeaderStatus.setVisibility(entries.isEmpty() ? View.VISIBLE : View.GONE);
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