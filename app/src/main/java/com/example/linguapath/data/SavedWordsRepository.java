package com.example.linguapath.data;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import com.example.linguapath.model.SavedWord;
import com.example.linguapath.model.Word;
import com.example.linguapath.provider.SavedWordsContract;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Screens never touch the database. They go through ContentResolver, here.
// Work runs on a background thread, answers come back on the main thread.
public class SavedWordsRepository {

    public interface BoolCallback {
        void onResult(boolean value);
    }

    public interface ListCallback {
        void onResult(List<SavedWord> words);
    }

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    private static ContentResolver resolver(Context context) {
        return context.getApplicationContext().getContentResolver();
    }

    // true = this word is already in the user's list
    public static void isSaved(Context context, String language, String word, BoolCallback callback) {
        executor.execute(() -> {
            boolean found = false;
            Cursor cursor = resolver(context).query(
                    SavedWordsContract.CONTENT_URI,
                    new String[]{SavedWordsContract.COLUMN_ID},
                    SavedWordsContract.COLUMN_LANGUAGE + " = ? AND "
                            + SavedWordsContract.COLUMN_WORD + " = ?",
                    new String[]{language, word}, null);
            if (cursor != null) {
                found = cursor.moveToFirst();
                cursor.close();
            }
            boolean result = found;
            mainHandler.post(() -> callback.onResult(result));
        });
    }

    // true = saved now, false = it was already saved
    public static void save(Context context, String language, Word word, BoolCallback callback) {
        executor.execute(() -> {
            ContentValues values = new ContentValues();
            values.put(SavedWordsContract.COLUMN_LANGUAGE, language);
            values.put(SavedWordsContract.COLUMN_WORD_ID, word.getId());
            values.put(SavedWordsContract.COLUMN_WORD, word.getWord());
            values.put(SavedWordsContract.COLUMN_MEANING, word.getMeaning());
            values.put(SavedWordsContract.COLUMN_EXAMPLE, word.getExample());

            Uri created = resolver(context).insert(SavedWordsContract.CONTENT_URI, values);
            boolean result = (created != null);
            mainHandler.post(() -> callback.onResult(result));
        });
    }

    // true = something was removed
    public static void remove(Context context, String language, String word, BoolCallback callback) {
        executor.execute(() -> {
            int rows = resolver(context).delete(
                    SavedWordsContract.CONTENT_URI,
                    SavedWordsContract.COLUMN_LANGUAGE + " = ? AND "
                            + SavedWordsContract.COLUMN_WORD + " = ?",
                    new String[]{language, word});
            boolean result = rows > 0;
            mainHandler.post(() -> callback.onResult(result));
        });
    }

    public static void removeById(Context context, long id, BoolCallback callback) {
        executor.execute(() -> {
            Uri item = ContentUris.withAppendedId(SavedWordsContract.CONTENT_URI, id);
            int rows = resolver(context).delete(item, null, null);
            boolean result = rows > 0;
            mainHandler.post(() -> callback.onResult(result));
        });
    }
    // A word the user typed themselves (not from the lessons). word_id = -1 marks it.
    public static void saveCustom(Context context, String language, String word,
                                  String meaning, BoolCallback callback) {
        executor.execute(() -> {
            ContentValues values = new ContentValues();
            values.put(SavedWordsContract.COLUMN_LANGUAGE, language);
            values.put(SavedWordsContract.COLUMN_WORD_ID, -1);
            values.put(SavedWordsContract.COLUMN_WORD, word);
            values.put(SavedWordsContract.COLUMN_MEANING, meaning);
            values.put(SavedWordsContract.COLUMN_EXAMPLE, "");

            Uri created = resolver(context).insert(SavedWordsContract.CONTENT_URI, values);
            boolean result = (created != null);
            mainHandler.post(() -> callback.onResult(result));
        });
    }
    public static void loadAll(Context context, ListCallback callback) {
        executor.execute(() -> {
            List<SavedWord> words = new ArrayList<>();
            Cursor cursor = resolver(context).query(
                    SavedWordsContract.CONTENT_URI, null, null, null, null);
            if (cursor != null) {
                try {
                    while (cursor.moveToNext()) {
                        words.add(new SavedWord(
                                cursor.getLong(cursor.getColumnIndexOrThrow(SavedWordsContract.COLUMN_ID)),
                                cursor.getInt(cursor.getColumnIndexOrThrow(SavedWordsContract.COLUMN_WORD_ID)),
                                cursor.getString(cursor.getColumnIndexOrThrow(SavedWordsContract.COLUMN_LANGUAGE)),
                                cursor.getString(cursor.getColumnIndexOrThrow(SavedWordsContract.COLUMN_WORD)),
                                cursor.getString(cursor.getColumnIndexOrThrow(SavedWordsContract.COLUMN_MEANING)),
                                cursor.getString(cursor.getColumnIndexOrThrow(SavedWordsContract.COLUMN_EXAMPLE)),
                                cursor.getLong(cursor.getColumnIndexOrThrow(SavedWordsContract.COLUMN_SAVED_AT))));
                    }
                } finally {
                    cursor.close();
                }
            }
            mainHandler.post(() -> callback.onResult(words));
        });
    }
}