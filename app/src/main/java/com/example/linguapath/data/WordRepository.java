package com.example.linguapath.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.example.linguapath.model.Word;

import java.util.ArrayList;
import java.util.List;

// Same idea as StageRepository: work in the background, answer on the main thread
public class WordRepository {

    public interface WordsCallback {
        void onWordsLoaded(List<Word> words);
    }

    public interface WordCallback {
        void onWordLoaded(Word word); // null if not found
    }

    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    private static Word toWord(WordEntity e) {
        return new Word(e.id, e.word, e.romanization, e.meaning, e.example, e.exampleMeaning);
    }

    public static void loadWords(Context context, int stageId, WordsCallback callback) {
        Context appContext = context.getApplicationContext();

        AppDatabase.databaseExecutor.execute(() -> {
            List<WordEntity> entities =
                    AppDatabase.getInstance(appContext).contentDao().getWordsForStage(stageId);
            List<Word> words = new ArrayList<>();
            for (WordEntity e : entities) {
                words.add(toWord(e));
            }
            mainHandler.post(() -> callback.onWordsLoaded(words));
        });
    }

    public static void loadWord(Context context, int wordId, WordCallback callback) {
        Context appContext = context.getApplicationContext();

        AppDatabase.databaseExecutor.execute(() -> {
            WordEntity entity =
                    AppDatabase.getInstance(appContext).contentDao().getWordById(wordId);
            Word word = (entity == null) ? null : toWord(entity);
            mainHandler.post(() -> callback.onWordLoaded(word));
        });
    }
}