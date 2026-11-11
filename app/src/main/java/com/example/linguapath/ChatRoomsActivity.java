package com.example.linguapath;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.widget.EditText;
import android.widget.FrameLayout;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.linguapath.adapter.ChatRoomAdapter;
import com.example.linguapath.data.ChatRepository;
import com.example.linguapath.data.ChatSettings;
import com.example.linguapath.databinding.ActivityChatRoomsBinding;
import com.example.linguapath.model.ChatRoom;
import com.example.linguapath.chat.ChatLevels;
import com.example.linguapath.data.ChatScoreRepository;

import java.util.regex.Pattern;

public class ChatRoomsActivity extends AppCompatActivity {

    private static final Pattern NICKNAME = Pattern.compile("^[A-Za-z0-9_]{3,16}$");

    private ActivityChatRoomsBinding binding;
    private ChatSettings settings;
    private String uid;
    private String nickname;
    private ChatRepository.Registration scoreRegistration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatRoomsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        settings = new ChatSettings(this);
        uid = ChatRepository.currentUid();
        if (uid == null) {
            finish(); // the chat needs a logged-in user
            return;
        }

        ChatRoomAdapter adapter = new ChatRoomAdapter(this::openRoom);
        binding.recyclerRooms.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerRooms.setAdapter(adapter);
        adapter.setRooms(ChatRoom.ALL);

        binding.buttonBack.setOnClickListener(v -> finish());
        binding.buttonChangeName.setOnClickListener(v -> showNicknameDialog(true));
        binding.buttonLeaderboard.setOnClickListener(v ->
                startActivity(new Intent(this, LeaderboardActivity.class)));

        nickname = settings.getNickname(uid);
        if (nickname != null) {
            showName();
        } else {
            loadNickname();
        }
    }

    // The nickname lives in the database, so it follows the user to another phone
    private void loadNickname() {
        binding.textYouAre.setText(R.string.chat_loading_name);
        ChatRepository.loadNickname((name, failed) -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            if (failed) {
                binding.textYouAre.setText(R.string.chat_name_load_failed);
            } else if (name != null) {
                nickname = name;
                settings.setNickname(uid, name);
                showName();
            } else {
                showNicknameDialog(false); // first time: the user must pick a name
            }
        });
    }

    private void showName() {
        binding.textYouAre.setText(getString(R.string.chat_you_are, nickname));
    }

    @Override
    protected void onStart() {
        super.onStart();
        scoreRegistration = ChatScoreRepository.listenMyScore(points ->
                binding.textMyScore.setText(getString(R.string.chat_my_score,
                        points, getString(ChatLevels.nameFor(points)))));
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (scoreRegistration != null) {
            scoreRegistration.remove();
            scoreRegistration = null;
        }
    }

    private void showNicknameDialog(boolean canCancel) {
        EditText input = new EditText(this);
        input.setHint(R.string.nickname_hint);
        input.setSingleLine(true);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(16)});
        if (nickname != null) {
            input.setText(nickname);
        }
        FrameLayout box = new FrameLayout(this);
        int pad = (int) (24 * getResources().getDisplayMetrics().density);
        box.setPadding(pad, pad / 3, pad, 0);
        box.addView(input);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.nickname_title)
                .setMessage(R.string.nickname_message)
                .setView(box)
                .setPositiveButton(R.string.nickname_save, null) // set below, so errors keep it open
                .setNegativeButton(android.R.string.cancel, (d, which) -> {
                    if (nickname == null) {
                        finish();
                    }
                })
                .setCancelable(canCancel)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String name = input.getText().toString().trim();
                    if (!NICKNAME.matcher(name).matches()) {
                        input.setError(getString(R.string.nickname_invalid));
                        return;
                    }
                    ChatRepository.saveNickname(name, ok -> {
                        if (!ok) {
                            input.setError(getString(R.string.nickname_error));
                            return;
                        }
                        nickname = name;
                        settings.setNickname(uid, name);
                        showName();
                        dialog.dismiss();
                    });
                }));
        dialog.show();
    }

    // Explicit Intent with extras
    private void openRoom(ChatRoom room) {
        if (nickname == null) {
            return; // no name yet
        }
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra(ChatActivity.EXTRA_ROOM_ID, room.id);
        intent.putExtra(ChatActivity.EXTRA_NICKNAME, nickname);
        startActivity(intent);
    }
}