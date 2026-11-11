package com.example.linguapath;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.linguapath.data.ChatRepository;
import com.example.linguapath.data.FriendRepository;
import com.example.linguapath.databinding.ActivityFriendsBinding;
import com.example.linguapath.databinding.ItemFriendBinding;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Friends: search, requests, friend list, private chat. Separate from the learning screens.
public class FriendsActivity extends AppCompatActivity {

    private ActivityFriendsBinding binding;
    private String nickname;
    private final Set<String> friendIds = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFriendsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.buttonBack.setOnClickListener(v -> finish());
        binding.buttonSearch.setOnClickListener(v -> search());
        binding.editSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                search();
                return true;
            }
            return false;
        });
        binding.buttonLeaderboard.setOnClickListener(v ->
                startActivity(new Intent(this, LeaderboardActivity.class)));

        ChatRepository.loadNickname((name, failed) -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            if (name == null) {
                binding.textSearchStatus.setText(R.string.friends_need_name);
            } else {
                nickname = name;
                ChatRepository.saveNickname(name, ok -> { }); // makes older accounts searchable too
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        refresh();
    }

    private boolean gone() {
        return isFinishing() || isDestroyed();
    }

    private void refresh() {
        FriendRepository.loadRequests(new FriendRepository.PeopleCallback() {
            @Override
            public void onLoaded(List<FriendRepository.Person> people) {
                if (gone()) {
                    return;
                }
                binding.containerRequests.removeAllViews();
                binding.textRequestsLabel.setVisibility(people.isEmpty() ? View.GONE : View.VISIBLE);
                for (FriendRepository.Person p : people) {
                    addRow(binding.containerRequests, p.name,
                            R.string.friends_accept, () -> FriendRepository.accept(p.uid, ok -> refresh()),
                            R.string.friends_decline, () -> FriendRepository.decline(p.uid, ok -> refresh()));
                }
            }

            @Override
            public void onFailed() {
            }
        });

        FriendRepository.loadFriends(new FriendRepository.PeopleCallback() {
            @Override
            public void onLoaded(List<FriendRepository.Person> people) {
                if (gone()) {
                    return;
                }
                friendIds.clear();
                binding.containerFriends.removeAllViews();
                binding.textFriendsStatus.setText(people.isEmpty() ? getString(R.string.friends_none) : "");
                for (FriendRepository.Person p : people) {
                    friendIds.add(p.uid);
                    addRow(binding.containerFriends, p.name,
                            R.string.friends_chat, () -> chooseLanguage(p),
                            R.string.friends_remove, () -> confirmRemove(p));
                }
            }

            @Override
            public void onFailed() {
                if (!gone()) {
                    binding.textFriendsStatus.setText(R.string.friends_failed);
                }
            }
        });
    }

    private void search() {
        String q = binding.editSearch.getText() == null
                ? "" : binding.editSearch.getText().toString().trim();
        binding.containerResults.removeAllViews();
        if (q.length() < 2) {
            binding.textSearchStatus.setText(R.string.friends_search_short);
            return;
        }
        binding.textSearchStatus.setText(R.string.friends_searching);
        FriendRepository.searchByName(q, new FriendRepository.PeopleCallback() {
            @Override
            public void onLoaded(List<FriendRepository.Person> people) {
                if (gone()) {
                    return;
                }
                int shown = 0;
                for (FriendRepository.Person p : people) {
                    if (friendIds.contains(p.uid)) {
                        continue;
                    }
                    shown++;
                    addRow(binding.containerResults, p.name, R.string.friends_add, () -> sendRequest(p), 0, null);
                }
                binding.textSearchStatus.setText(shown == 0 ? getString(R.string.friends_no_results) : "");
            }

            @Override
            public void onFailed() {
                if (!gone()) {
                    binding.textSearchStatus.setText(R.string.friends_failed);
                }
            }
        });
    }

    private void sendRequest(FriendRepository.Person p) {
        if (nickname == null) {
            Toast.makeText(this, R.string.friends_need_name, Toast.LENGTH_SHORT).show();
            return;
        }
        FriendRepository.sendRequest(p.uid, nickname, ok -> {
            if (gone()) {
                return;
            }
            if (ok) {
                Toast.makeText(this, getString(R.string.friends_request_sent, p.name), Toast.LENGTH_SHORT).show();
                binding.containerResults.removeAllViews();
                binding.textSearchStatus.setText("");
            } else {
                Toast.makeText(this, R.string.friends_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void chooseLanguage(FriendRepository.Person friend) {
        if (nickname == null) {
            Toast.makeText(this, R.string.friends_need_name, Toast.LENGTH_SHORT).show();
            return;
        }
        String[] languages = {"Spanish", "Korean"};
        new AlertDialog.Builder(this)
                .setTitle(R.string.friends_chat_language)
                .setItems(languages, (dialog, which) -> {
                    Intent intent = new Intent(this, ChatActivity.class);
                    intent.putExtra(ChatActivity.EXTRA_ROOM_ID,
                            FriendRepository.dmRoomId(ChatRepository.currentUid(), friend.uid));
                    intent.putExtra(ChatActivity.EXTRA_NICKNAME, nickname);
                    intent.putExtra(ChatActivity.EXTRA_FRIEND_NAME, friend.name);
                    intent.putExtra(ChatActivity.EXTRA_LANGUAGE, languages[which]);
                    startActivity(intent);
                })
                .show();
    }

    private void confirmRemove(FriendRepository.Person friend) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.friends_remove_title)
                .setMessage(getString(R.string.friends_remove_msg, friend.name))
                .setPositiveButton(R.string.friends_remove, (d, w) ->
                        FriendRepository.remove(friend.uid, ok -> refresh()))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    // secondaryLabel = 0 hides the second button
    private void addRow(LinearLayout container, String name, int primaryLabel, Runnable primary,
                        int secondaryLabel, Runnable secondary) {
        ItemFriendBinding row = ItemFriendBinding.inflate(getLayoutInflater(), container, false);
        row.textFriendName.setText(name);
        row.buttonRowPrimary.setText(primaryLabel);
        row.buttonRowPrimary.setOnClickListener(v -> primary.run());
        if (secondaryLabel == 0) {
            row.buttonRowSecondary.setVisibility(View.GONE);
        } else {
            row.buttonRowSecondary.setText(secondaryLabel);
            row.buttonRowSecondary.setOnClickListener(v -> secondary.run());
        }
        container.addView(row.getRoot());
    }
}