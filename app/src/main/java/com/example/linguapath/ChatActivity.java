package com.example.linguapath;

import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.linguapath.adapter.ChatMessageAdapter;
import com.example.linguapath.data.ChatRepository;
import com.example.linguapath.data.ChatSettings;
import com.example.linguapath.databinding.ActivityChatBinding;
import com.example.linguapath.model.ChatMessage;
import com.example.linguapath.model.ChatRoom;
import com.example.linguapath.chat.ChatLevels;
import com.example.linguapath.chat.VocabMatcher;
import com.example.linguapath.data.ChatScoreRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import android.widget.TextView;

public class ChatActivity extends AppCompatActivity {

    public static final String EXTRA_ROOM_ID = "extra_room_id";
    public static final String EXTRA_NICKNAME = "extra_nickname";
    private static final long MIN_SEND_GAP_MS = 2000;

    private ActivityChatBinding binding;
    private ChatMessageAdapter adapter;
    private LinearLayoutManager layoutManager;
    private ChatSettings settings;
    private ChatRoom room;
    private String nickname;
    private String myUid;
    private long lastSendAt = -MIN_SEND_GAP_MS;
    private List<String> knownWords = new ArrayList<>();
    private String lastScoredText = "";
    private ChatRepository.Registration scoreRegistration;

    private ChatRepository.Registration messageRegistration;
    private ChatRepository.Registration connectionRegistration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        settings = new ChatSettings(this);
        room = ChatRoom.byId(getIntent().getStringExtra(EXTRA_ROOM_ID));
        nickname = getIntent().getStringExtra(EXTRA_NICKNAME);
        myUid = ChatRepository.currentUid();
        if (room == null || nickname == null || myUid == null) {
            finish();
            return;
        }

        binding.textRoomTitle.setText(room.titleRes);
        binding.editMessage.setHint(getString(R.string.chat_input_hint, room.language));
        binding.buttonBack.setOnClickListener(v -> finish());
        binding.buttonSend.setOnClickListener(v -> send());

        adapter = new ChatMessageAdapter(myUid, this::showOptions);
        layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true); // the newest message is at the bottom
        binding.recyclerMessages.setLayoutManager(layoutManager);
        binding.recyclerMessages.setAdapter(adapter);

        // Which words does this user know? (for the marks)
        ChatScoreRepository.loadKnownWords(this, room.language, words -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            knownWords = words;
            showHint();
        });
    }

    // Listen only while the screen is visible
    @Override
    protected void onStart() {
        super.onStart();
        adapter.clear(); // the database sends the last messages again when we listen again
        updateEmptyState();
        messageRegistration = ChatRepository.listen(room.id, this::onMessage);
        connectionRegistration = ChatRepository.listenConnection(connected ->
                binding.textConnection.setText(connected ? R.string.chat_online : R.string.chat_offline));

        scoreRegistration = ChatScoreRepository.listenMyScore(points ->
                binding.textMyScore.setText(getString(R.string.chat_my_score,
                        points, getString(ChatLevels.nameFor(points)))));
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (messageRegistration != null) {
            messageRegistration.remove();
            messageRegistration = null;
        }
        if (connectionRegistration != null) {
            connectionRegistration.remove();
            connectionRegistration = null;
        }
        if (scoreRegistration != null) {
            scoreRegistration.remove();
            scoreRegistration = null;
        }
    }

    private void onMessage(ChatMessage message) {
        if (settings.isMuted(message.uid)) {
            return; // hidden on this phone
        }
        boolean nearBottom = layoutManager.findLastVisibleItemPosition() >= adapter.getItemCount() - 2;
        adapter.add(message);
        updateEmptyState();
        if (nearBottom || message.uid.equals(myUid)) {
            binding.recyclerMessages.scrollToPosition(adapter.getItemCount() - 1);
        }
    }

    private void updateEmptyState() {
        binding.textEmpty.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }

    // ---- Sending ----

    private void send() {
        String text = binding.editMessage.getText() == null
                ? "" : binding.editMessage.getText().toString().trim();
        binding.textChatError.setText("");
        if (text.isEmpty()) {
            return;
        }

        long now = SystemClock.elapsedRealtime();
        if (now - lastSendAt < MIN_SEND_GAP_MS) {
            binding.textChatError.setText(R.string.chat_too_fast);
            return;
        }
        lastSendAt = now;

        binding.editMessage.setText("");
        final String sentText = text;
        ChatRepository.sendMessage(room.id, nickname, text, success -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            if (success) {
                scoreMessage(sentText);
            } else {
                binding.textChatError.setText(R.string.chat_send_failed);
            }
        });
    }

    // ---- Safety: report or hide (only for other people's messages) ----

    private void showOptions(ChatMessage message) {
        if (message.uid.equals(myUid)) {
            return;
        }
        CharSequence[] options = {
                getString(R.string.chat_options_report),
                getString(R.string.chat_options_hide, message.name)};

        new AlertDialog.Builder(this)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        confirmReport(message);
                    } else {
                        settings.mute(message.uid);
                        adapter.removeByUid(message.uid);
                        updateEmptyState();
                        binding.textChatError.setText(getString(R.string.chat_hidden, message.name));
                    }
                })
                .show();
    }
    // The marking rule: 2 points for each known word used (max 3 words), max 30 points a day
    private void scoreMessage(String text) {
        if (text.equalsIgnoreCase(lastScoredText)) {
            return; // the same message twice earns nothing
        }
        lastScoredText = text;

        List<String> found = VocabMatcher.findWords(text, knownWords, room.language);
        int points = VocabMatcher.pointsFor(found);

        ChatSettings s = settings;
        int remaining = ChatSettings.DAILY_POINT_CAP - s.getPointsToday();
        points = Math.min(points, remaining);
        if (points <= 0) {
            return;
        }
        s.addPointsToday(points);
        ChatScoreRepository.award(nickname, points);
        showPointsPopup(points);
    }

    // "+4 ✨" floats up and fades away
    private void showPointsPopup(int points) {
        TextView popup = binding.textPointsPopup;
        popup.setText(getString(R.string.chat_points_popup, points));
        popup.setVisibility(View.VISIBLE);
        popup.setAlpha(0f);
        popup.setTranslationY(24 * getResources().getDisplayMetrics().density);
        popup.animate().alpha(1f).translationY(0f).setDuration(300)
                .withEndAction(() -> popup.animate().alpha(0f).setStartDelay(1200).setDuration(400)
                        .withEndAction(() -> {
                            popup.setVisibility(View.INVISIBLE);
                            popup.animate().setStartDelay(0);
                        }));
    }

    // A nudge: three words you know that you could use
    private void showHint() {
        if (knownWords.isEmpty()) {
            binding.textHint.setText("");
            return;
        }
        List<String> copy = new ArrayList<>(knownWords);
        Collections.shuffle(copy);
        binding.textHint.setText(getString(R.string.chat_hint_words,
                String.join(", ", copy.subList(0, Math.min(3, copy.size())))));
    }
    private void confirmReport(ChatMessage message) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.chat_report_title)
                .setMessage(R.string.chat_report_message)
                .setPositiveButton(R.string.chat_report_yes, (dialog, which) ->
                        ChatRepository.report(room.id, message.id, ok -> {
                            if (!isFinishing() && !isDestroyed()) {
                                binding.textChatError.setText(
                                        ok ? R.string.chat_reported : R.string.chat_report_failed);
                            }
                        }))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}