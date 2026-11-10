package com.example.linguapath.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuizHistoryRepository {

    public interface BoolCallback {
        void onResult(boolean value);
    }

    public interface BestCallback {
        void onResult(Map<Integer, Integer> bestByStage);
    }

    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static String currentUserId() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return (user == null) ? "guest" : user.getUid();
    }

    // Saves the attempt. Answers true if it beats the previous best score for that stage.
    public static void record(Context context, QuizAttempt attempt, BoolCallback callback) {
        Context appContext = context.getApplicationContext();
        AppDatabase.databaseExecutor.execute(() -> {
            QuizAttemptDao dao = AppDatabase.getInstance(appContext).quizAttemptDao();
            Integer before = dao.bestPercent(attempt.userId, attempt.language, attempt.stageIndex);
            dao.insert(attempt);
            boolean newBest = (before == null) || attempt.percent > before;
            mainHandler.post(() -> callback.onResult(newBest));
        });
    }

    public static void loadBestScores(Context context, String language, BestCallback callback) {
        Context appContext = context.getApplicationContext();
        AppDatabase.databaseExecutor.execute(() -> {
            List<BestScore> rows = AppDatabase.getInstance(appContext)
                    .quizAttemptDao().bestScores(currentUserId(), language);
            Map<Integer, Integer> map = new HashMap<>();
            for (BestScore row : rows) {
                map.put(row.stageIndex, row.best);
            }
            mainHandler.post(() -> callback.onResult(map));
        });
    }
}