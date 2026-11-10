package com.example.linguapath.quiz;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.example.linguapath.data.AppDatabase;
import com.example.linguapath.data.ContentDao;
import com.example.linguapath.data.WordEntity;
import com.example.linguapath.model.Word;
import com.example.linguapath.data.QuizHistoryRepository;
import com.example.linguapath.data.WordStat;
import com.example.linguapath.data.WordStatDao;

import java.util.ArrayList;
import java.util.List;

public class QuizRepository {

    public interface Callback {
        void onLoaded(List<QuizQuestion> questions);
    }

    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static void loadQuiz(Context context, int stageId, String language, Callback callback) {
        Context appContext = context.getApplicationContext();

        // The same single worker as the rest of the database, so the lesson import is finished first
        AppDatabase.databaseExecutor.execute(() -> {
            ContentDao dao = AppDatabase.getInstance(appContext).contentDao();
            List<Word> stageWords = toWords(dao.getWordsForStage(stageId));
            List<Word> pool = toWords(dao.getWordsForLanguage(language));

            List<QuizQuestion> questions = QuizBuilder.build(stageWords, pool);
            mainHandler.post(() -> callback.onLoaded(questions));
        });
    }
    // A quiz made only of the words this user needs to revise
    public static void loadRevisionQuiz(Context context, String language, Callback callback) {
        Context appContext = context.getApplicationContext();

        AppDatabase.databaseExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(appContext);
            String userId = QuizHistoryRepository.currentUserId();

            List<WordStat> due = RevisionPlanner.pick(
                    db.wordStatDao().candidates(userId, language), System.currentTimeMillis());

            List<Word> words = new ArrayList<>();
            for (WordStat stat : due) {
                WordEntity e = db.contentDao().getWordById(stat.wordId);
                if (e != null) {
                    words.add(new Word(e.id, e.word, e.romanization, e.meaning, e.example, e.exampleMeaning));
                }
            }
            List<Word> pool = toWords(db.contentDao().getWordsForLanguage(language));

            List<QuizQuestion> questions = QuizBuilder.build(words, pool);
            mainHandler.post(() -> callback.onLoaded(questions));
        });
    }
    private static List<Word> toWords(List<WordEntity> entities) {
        List<Word> words = new ArrayList<>();
        for (WordEntity e : entities) {
            words.add(new Word(e.id, e.word, e.romanization, e.meaning, e.example, e.exampleMeaning));
        }
        return words;
    }
}