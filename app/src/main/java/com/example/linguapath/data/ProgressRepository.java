package com.example.linguapath.data;

import android.content.Context;
import android.database.Cursor;
import android.os.Handler;
import android.os.Looper;

import com.example.linguapath.R;
import com.example.linguapath.model.ProgressSnapshot;
import com.example.linguapath.provider.SavedWordsContract;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

public class ProgressRepository {

    public interface Callback {
        void onLoaded(ProgressSnapshot snapshot);
    }

    // To add a language later, add its name here (and its lessons to content.json)
    private static final String[] LANGUAGES = {"Spanish", "Korean", "Bangla", "Japanese", "French"};

    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static void load(Context context, Callback callback) {
        Context appContext = context.getApplicationContext();

        AppDatabase.databaseExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(appContext);
            DataImporter.importIfNeeded(appContext, db); // in case this is the very first launch

            String userId = QuizHistoryRepository.currentUserId();
            ProgressStore store = new ProgressStore(appContext);
            ProgressSnapshot s = new ProgressSnapshot();

            s.xp = store.getXp();
            s.studyMinutesToday = (int) (store.getStudyMillisToday() / 60000);

            int languagesWithProgress = 0;
            for (String language : LANGUAGES) {
                int total = db.contentDao().countStagesForLanguage(language);
                int done = Math.min(store.getCompletedCount(language), total);
                ProgressSnapshot.LanguageProgress lp =
                        new ProgressSnapshot.LanguageProgress(language, done, total);

                // Words that need revision (see RevisionPlanner for the rule)
                java.util.List<WordStat> due = com.example.linguapath.quiz.RevisionPlanner.pick(
                        db.wordStatDao().candidates(userId, language), System.currentTimeMillis());
                lp.revisionCount = due.size();
                StringBuilder preview = new StringBuilder();
                for (int i = 0; i < due.size() && i < 3; i++) {
                    WordEntity word = db.contentDao().getWordById(due.get(i).wordId);
                    if (word != null) {
                        if (preview.length() > 0) {
                            preview.append(", ");
                        }
                        preview.append(word.word);
                    }
                }
                lp.revisionPreview = preview.toString();

                s.languages.add(lp);
                if (done > 0) {
                    languagesWithProgress++;
                }
            }

            QuizAttemptDao attempts = db.quizAttemptDao();
            s.attempts = attempts.latest(userId, 20);
            s.quizCount = attempts.count(userId);
            s.perfectCount = attempts.perfectCount(userId);

            // Streak: the days with a quiz or study time
            Set<String> days = new HashSet<>(attempts.attemptDays(userId));
            days.addAll(store.getStudyDays());
            s.streak = streak(days);

            s.savedWords = countSavedWords(appContext);

            // Achievements are worked out from the numbers above, nothing extra is stored
            int stagesPassed = store.getTotalCompleted();
            s.achievements.add(new ProgressSnapshot.Achievement(
                    appContext.getString(R.string.ach_first_title),
                    appContext.getString(R.string.ach_first_desc), stagesPassed >= 1));
            s.achievements.add(new ProgressSnapshot.Achievement(
                    appContext.getString(R.string.ach_perfect_title),
                    appContext.getString(R.string.ach_perfect_desc), s.perfectCount >= 1));
            s.achievements.add(new ProgressSnapshot.Achievement(
                    appContext.getString(R.string.ach_streak_title),
                    appContext.getString(R.string.ach_streak_desc), s.streak >= 3));
            s.achievements.add(new ProgressSnapshot.Achievement(
                    appContext.getString(R.string.ach_saver_title),
                    appContext.getString(R.string.ach_saver_desc), s.savedWords >= 10));
            s.achievements.add(new ProgressSnapshot.Achievement(
                    appContext.getString(R.string.ach_quiz_title),
                    appContext.getString(R.string.ach_quiz_desc), s.quizCount >= 10));
            s.achievements.add(new ProgressSnapshot.Achievement(
                    appContext.getString(R.string.ach_both_title),
                    appContext.getString(R.string.ach_both_desc),
                    languagesWithProgress >= LANGUAGES.length));

            mainHandler.post(() -> callback.onLoaded(s));
        });
    }

    // Counts the days in a row, ending today. If you have not studied yet today,
    // we start from yesterday, so the streak is not lost before the day is over.
    static int streak(Set<String> days) {
        LocalDate day = LocalDate.now();
        if (!days.contains(day.toString())) {
            day = day.minusDays(1);
        }
        int count = 0;
        while (days.contains(day.toString())) {
            count++;
            day = day.minusDays(1);
        }
        return count;
    }

    // Asks the Content Provider (through ContentResolver) how many words are saved
    private static int countSavedWords(Context context) {
        Cursor cursor = context.getContentResolver().query(
                SavedWordsContract.CONTENT_URI,
                new String[]{SavedWordsContract.COLUMN_ID}, null, null, null);
        int count = 0;
        if (cursor != null) {
            count = cursor.getCount();
            cursor.close();
        }
        return count;
    }
}