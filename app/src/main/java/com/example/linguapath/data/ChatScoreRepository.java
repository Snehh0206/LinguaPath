package com.example.linguapath.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.linguapath.model.Topic;
import com.example.linguapath.model.TopicWord;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Query;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Chat points: which words the learner knows, adding points, my score, and the Top 10
public class ChatScoreRepository {

    public interface WordsCallback {
        void onLoaded(List<String> words);
    }

    public interface LeaderboardCallback {
        void onLoaded(List<Entry> entries);

        void onFailed();
    }

    public static class Entry {
        public final String uid;
        public final String name;
        public final int points;

        public Entry(String uid, String name, int points) {
            this.uid = uid;
            this.name = name;
            this.points = points;
        }
    }

    public interface ScoreListener {
        void onScore(int points);
    }

    private static final String TAG = "ChatScoreRepository";
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    private static DatabaseReference scores() {
        return FirebaseDatabase.getInstance().getReference("scores");
    }

    // The words this user knows in a language: finished stages plus all Topic Words
    public static void loadKnownWords(Context context, String language, WordsCallback callback) {
        Context appContext = context.getApplicationContext();
        AppDatabase.databaseExecutor.execute(() -> {
            int learnedStages = new ProgressStore(appContext).getCompletedCount(language);
            List<String> words = new ArrayList<>(
                    AppDatabase.getInstance(appContext).contentDao()
                            .getLearnedWordTexts(language, learnedStages));
            for (Topic topic : TopicRepository.loadBlocking(appContext)) {
                for (TopicWord topicWord : topic.getWords(language)) {
                    words.add(topicWord.word);
                }
            }
            mainHandler.post(() -> callback.onLoaded(words));
        });
    }

    // Adds points to my total. A transaction reads the current number and writes number + points.
    public static void award(String nickname, int points) {
        String uid = ChatRepository.currentUid();
        if (uid == null || points <= 0) {
            return;
        }
        DatabaseReference mine = scores().child(uid);
        mine.child("name").setValue(nickname);
        mine.child("points").runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData data) {
                Long current = data.getValue(Long.class);
                data.setValue((current == null ? 0L : current) + points);
                return Transaction.success(data);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed, DataSnapshot snapshot) {
                if (error != null) {
                    Log.w(TAG, "Could not add points: " + error.getMessage());
                }
            }
        });
    }

    // My score, updated live. Call remove() in onStop.
    public static ChatRepository.Registration listenMyScore(ScoreListener listener) {
        String uid = ChatRepository.currentUid();
        if (uid == null) {
            return () -> { };
        }
        DatabaseReference ref = scores().child(uid).child("points");
        ValueEventListener valueListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Long value = snapshot.getValue(Long.class);
                listener.onScore(value == null ? 0 : value.intValue());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "Score cancelled: " + error.getMessage());
            }
        };
        ref.addValueEventListener(valueListener);
        return () -> ref.removeEventListener(valueListener);
    }

    // The 10 highest scores, best first
    public static void loadLeaderboard(LeaderboardCallback callback) {
        Query query = scores().orderByChild("points").limitToLast(10);
        query.get().addOnCompleteListener(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                callback.onFailed();
                return;
            }
            List<Entry> entries = new ArrayList<>();
            for (DataSnapshot child : task.getResult().getChildren()) {
                String name = child.child("name").getValue(String.class);
                Long points = child.child("points").getValue(Long.class);
                if (name != null && points != null && child.getKey() != null) {
                    entries.add(new Entry(child.getKey(), name, points.intValue()));
                }
            }
            Collections.sort(entries, (a, b) -> Integer.compare(b.points, a.points)); // highest first
            callback.onLoaded(entries);
        });
    }
}