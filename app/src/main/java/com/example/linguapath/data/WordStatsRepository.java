package com.example.linguapath.data;

import android.content.Context;

import java.util.List;

public class WordStatsRepository {

    // Called when a quiz finishes. Runs on the same background worker as the other database work.
    public static void recordAnswers(Context context, String language,
                                     List<Integer> correctIds, List<Integer> wrongIds) {
        Context appContext = context.getApplicationContext();
        AppDatabase.databaseExecutor.execute(() -> {
            WordStatDao dao = AppDatabase.getInstance(appContext).wordStatDao();
            String userId = QuizHistoryRepository.currentUserId();
            long now = System.currentTimeMillis();

            for (int wordId : correctIds) {
                update(dao, userId, language, wordId, true, now);
            }
            for (int wordId : wrongIds) {
                update(dao, userId, language, wordId, false, now);
            }
        });
    }

    private static void update(WordStatDao dao, String userId, String language,
                               int wordId, boolean correct, long now) {
        WordStat stat = dao.find(userId, wordId);
        if (stat == null) {
            stat = new WordStat(userId, language, wordId, 0, 0, now);
        }
        if (correct) {
            stat.correctCount++;
        } else {
            stat.wrongCount++;
        }
        stat.lastSeen = now;
        dao.upsert(stat);
    }
}