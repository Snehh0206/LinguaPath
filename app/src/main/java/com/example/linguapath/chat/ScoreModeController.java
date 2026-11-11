package com.example.linguapath.chat;

import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.linguapath.R;
import com.example.linguapath.data.ChatRepository;
import com.example.linguapath.data.ChatScoreRepository;
import com.example.linguapath.data.ScoreSessionRepository;
import com.example.linguapath.data.TopicRepository;
import com.example.linguapath.databinding.ActivityChatBinding;
import com.example.linguapath.model.Topic;
import com.example.linguapath.model.TopicWord;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;

// Score Mode for a private chat: request, shared scenario, 5 minutes, live scoring, rewards
public class ScoreModeController {

    private static final long TICK_MS = 1000;
    private static final long BATCH_WAIT_MS = 25000;       // 2 messages waiting this long are scored
    private static final long REQUEST_EXPIRES_MS = 10 * 60 * 1000L;
    private static final int REWARD_WIN = 10;
    private static final int REWARD_DRAW = 5;
    private static final int REWARD_LOSE = 2;

    private final AppCompatActivity activity;
    private final ActivityChatBinding b;
    private final String pair;
    private final String myUid;
    private final String friendUid;
    private final String friendName;
    private final String nickname;
    private final Consumer<String> popup;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private ChatRepository.Registration sessionReg;
    private ChatRepository.Registration offsetReg;
    private long offset = 0;
    private ScoreSessionRepository.Session session;

    private List<String> sessionKnown = new ArrayList<>();
    private final List<String> scenarioWords = new ArrayList<>();
    private String scenarioLabel = "";
    private String wordsLoadedFor;
    private String requestDialogFor;
    private String finishedFor;
    private final List<String> buffer = new ArrayList<>();
    private final Set<String> usedMessages = new HashSet<>();

    private final Runnable evaluateLater = this::evaluate;
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            render();
            handler.postDelayed(this, TICK_MS);
        }
    };

    public ScoreModeController(AppCompatActivity activity, ActivityChatBinding binding, String pair,
                               String myUid, String friendUid, String friendName, String nickname,
                               Consumer<String> popup) {
        this.activity = activity;
        this.b = binding;
        this.pair = pair;
        this.myUid = myUid;
        this.friendUid = friendUid;
        this.friendName = friendName;
        this.nickname = nickname;
        this.popup = popup;
    }

    public void start() {
        offsetReg = ScoreSessionRepository.listenOffset(o -> offset = o);
        sessionReg = ScoreSessionRepository.listen(pair, this::onSession);
        handler.post(tick);
    }

    public void stop() {
        if (offsetReg != null) {
            offsetReg.remove();
            offsetReg = null;
        }
        if (sessionReg != null) {
            sessionReg.remove();
            sessionReg = null;
        }
        handler.removeCallbacksAndMessages(null);
    }

    // ---- State helpers ----

    private long now() {
        return System.currentTimeMillis() + offset;
    }

    private int pts(String uid) {
        if (session == null) {
            return 0;
        }
        Integer value = session.pts.get(uid);
        return value == null ? 0 : value;
    }

    private boolean isActive() {
        return session != null && "active".equals(session.state) && session.sid != null && session.startedAt > 0;
    }

    private boolean reached() {
        return pts(myUid) >= ScoreSessionRepository.TARGET || pts(friendUid) >= ScoreSessionRepository.TARGET;
    }

    private boolean isLive() {
        return isActive() && !reached() && now() < session.startedAt + ScoreSessionRepository.DURATION_MS;
    }

    // a small gap after 5:00 lets the last points arrive before the result is read
    private boolean isOver() {
        return isActive() && (reached() || now() >= session.startedAt + ScoreSessionRepository.DURATION_MS + 2000);
    }

    private boolean requestExpired() {
        return session.requestedAt > 0 && now() - session.requestedAt > REQUEST_EXPIRES_MS;
    }

    private boolean waitingForFriend() {
        return session != null && "requested".equals(session.state)
                && myUid.equals(session.requester) && !requestExpired();
    }

    private boolean gone() {
        return activity.isFinishing() || activity.isDestroyed();
    }

    // ---- Session changes ----

    private void onSession(ScoreSessionRepository.Session s) {
        session = s;
        if (s != null && s.sid != null) {
            if ("requested".equals(s.state) && !myUid.equals(s.requester) && !requestExpired()
                    && !s.sid.equals(requestDialogFor)) {
                requestDialogFor = s.sid;
                showRequestDialog();
            }
            if (isActive() && !s.sid.equals(wordsLoadedFor)) {
                loadWords();
            }
        }
        render();
    }

    private void loadWords() {
        wordsLoadedFor = session.sid;
        String language = session.language;
        String topicId = session.scenario;
        ChatScoreRepository.loadKnownWords(activity, language, words -> sessionKnown = words);
        TopicRepository.load(activity, topics -> {
            for (Topic t : topics) {
                if (t.id.equals(topicId)) {
                    scenarioLabel = t.emoji + " " + t.title;
                    scenarioWords.clear();
                    for (TopicWord w : t.getWords(language)) {
                        scenarioWords.add(w.word);
                    }
                }
            }
            render();
        });
    }

    // ---- The pinned score bar ----

    private void render() {
        if (gone()) {
            return;
        }
        if (session == null || (!isActive() && !waitingForFriend())) {
            b.layoutScore.setVisibility(View.GONE);
            return;
        }
        b.layoutScore.setVisibility(View.VISIBLE);

        if (!isActive()) { // waiting for the friend to accept
            b.textScenario.setText(activity.getString(R.string.score_waiting, friendName));
            b.textTimer.setText("");
            b.textScenarioWords.setText("");
            b.layoutBars.setVisibility(View.GONE);
            return;
        }

        b.layoutBars.setVisibility(View.VISIBLE);
        b.textScenario.setText(activity.getString(R.string.score_scenario, scenarioLabel));
        b.textScenarioWords.setText(scenarioWords.isEmpty() ? ""
                : activity.getString(R.string.score_words, String.join(", ", scenarioWords)));

        int mine = pts(myUid);
        int theirs = pts(friendUid);
        b.progressYou.setProgress(mine);
        b.progressFriend.setProgress(theirs);
        b.textYouScore.setText(activity.getString(R.string.score_you, mine, ScoreSessionRepository.TARGET));
        b.textFriendScore.setText(activity.getString(R.string.score_friend, friendName, theirs,
                ScoreSessionRepository.TARGET));

        if (isLive()) {
            long left = Math.max(0, session.startedAt + ScoreSessionRepository.DURATION_MS - now());
            b.textTimer.setText(String.format(Locale.ROOT, "⏱ %d:%02d", left / 60000, (left / 1000) % 60));
        } else {
            b.textTimer.setText(R.string.score_finished);
        }

        if (isOver() && !session.sid.equals(finishedFor)) {
            finishMatch();
        }
    }

    // ---- Result and reward (paid once) ----

    private void finishMatch() {
        finishedFor = session.sid;
        final int mine = pts(myUid);
        final int theirs = pts(friendUid);
        if (mine == 0 && theirs == 0) {
            return; // nobody played
        }
        final int reward = mine == 0 ? 0 : (mine > theirs ? REWARD_WIN : mine == theirs ? REWARD_DRAW : REWARD_LOSE);
        ScoreSessionRepository.claimReward(pair, session.sid, claimed -> {
            if (!claimed) {
                return; // already paid before (for example before the app was closed)
            }
            if (reward > 0) {
                ChatScoreRepository.award(nickname, reward);
            }
            if (gone()) {
                return;
            }
            String result = mine > theirs ? activity.getString(R.string.score_won)
                    : mine < theirs ? activity.getString(R.string.score_lost, friendName)
                    : activity.getString(R.string.score_draw);
            if (reward > 0) {
                result += "\n" + activity.getString(R.string.score_reward_line, reward);
            }
            new AlertDialog.Builder(activity)
                    .setTitle(R.string.score_result_title)
                    .setMessage(result)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        });
    }

    // ---- Request flow ----

    private void showRequestDialog() {
        new AlertDialog.Builder(activity)
                .setTitle(R.string.score_request_title)
                .setMessage(activity.getString(R.string.score_request_message, friendName, session.language))
                .setCancelable(false)
                .setPositiveButton(R.string.score_accept, (d, w) ->
                        ScoreSessionRepository.accept(pair, ok -> {
                            if (!ok && !gone()) {
                                Toast.makeText(activity, R.string.score_accept_failed, Toast.LENGTH_LONG).show();
                            }
                        }))
                .setNegativeButton(R.string.score_decline, (d, w) ->
                        ScoreSessionRepository.decline(pair, ok -> { }))
                .show();
    }

    // The three-dot button of the chat
    public void showMenu(View anchor) {
        PopupMenu menu = new PopupMenu(activity, anchor);
        if (waitingForFriend()) {
            menu.getMenu().add(0, 2, 0, R.string.score_cancel);
        } else {
            menu.getMenu().add(0, 1, 0, R.string.score_menu_start);
        }
        menu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 2) {
                ScoreSessionRepository.decline(pair, ok -> { });
            } else if (isLive()) {
                Toast.makeText(activity, R.string.score_running, Toast.LENGTH_SHORT).show();
            } else {
                chooseLanguage();
            }
            return true;
        });
        menu.show();
    }

    private void chooseLanguage() {
        String[] languages = {"Spanish", "Korean", "Bangla", "Japanese", "French"};
        new AlertDialog.Builder(activity)
                .setTitle(R.string.score_pick_language)
                .setItems(languages, (d, which) -> tryRequest(languages[which]))
                .show();
    }

    private void tryRequest(String language) {
        ScoreSessionRepository.loadCooldownStart(pair, start -> {
            if (gone()) {
                return;
            }
            long left = start == null ? 0 : start + ScoreSessionRepository.COOLDOWN_MS - now();
            if (left > 0) {
                long minutes = (left + 59999) / 60000;
                Toast.makeText(activity, activity.getString(R.string.score_cooldown, friendName,
                        (minutes / 60) + "h " + (minutes % 60) + "m"), Toast.LENGTH_LONG).show();
                return;
            }
            TopicRepository.load(activity, topics -> {
                if (topics.isEmpty() || gone()) {
                    return;
                }
                Topic topic = topics.get(new Random().nextInt(topics.size()));
                ScoreSessionRepository.request(pair, language, topic.id, ok -> {
                    if (!gone()) {
                        Toast.makeText(activity, ok
                                ? activity.getString(R.string.score_request_sent, friendName)
                                : activity.getString(R.string.score_request_failed), Toast.LENGTH_LONG).show();
                    }
                });
            });
        });
    }

    // ---- Scoring ----

    // true = a match is running and this message was handled here
    public boolean onMyMessage(String text) {
        if (!isLive()) {
            return false;
        }
        String normal = text.trim().toLowerCase(Locale.ROOT);
        if (!isMeaningful(normal)) {
            return true; // counts for nothing
        }
        usedMessages.add(normal);
        buffer.add(text);
        handler.removeCallbacks(evaluateLater);
        if (buffer.size() >= 3) {
            evaluate();
        } else if (buffer.size() == 2) {
            handler.postDelayed(evaluateLater, BATCH_WAIT_MS);
        }
        return true;
    }

    // No spam: at least 2 words, not too short, not repeated letters, not the same message again
    private boolean isMeaningful(String normal) {
        if (normal.length() < 4 || normal.split("\\s+").length < 2 || usedMessages.contains(normal)) {
            return false;
        }
        Set<Character> letters = new HashSet<>();
        for (char c : normal.toCharArray()) {
            if (Character.isLetter(c)) {
                letters.add(c);
            }
        }
        return letters.size() >= 4;
    }

    private void evaluate() {
        if (buffer.size() < 2 || !isLive()) {
            buffer.clear();
            return;
        }
        String joined = String.join(" ", buffer);
        buffer.clear();
        String language = session.language;
        int scenarioUsed = VocabMatcher.findWords(joined, scenarioWords, language).size();
        int knownUsed = VocabMatcher.findWords(joined, sessionKnown, language).size();

        int gained = (scenarioUsed >= 2 || (scenarioUsed >= 1 && knownUsed >= 3)) ? 2
                : (scenarioUsed >= 1 || knownUsed >= 2) ? 1 : 0;
        gained = Math.min(gained, ScoreSessionRepository.TARGET - pts(myUid));
        popup.accept("+" + Math.max(gained, 0));
        if (gained > 0) {
            ScoreSessionRepository.addPoints(pair, session.sid, gained);
        }
    }
}