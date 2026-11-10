package com.example.linguapath.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.time.LocalDate;

public class WordOfDayRepository {

    public interface Callback {
        void onResult(WordOfDay word); // null when there is no word for today yet
    }

    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static void loadToday(Context context, String language, Callback callback) {
        Context appContext = context.getApplicationContext();
        AppDatabase.databaseExecutor.execute(() -> {
            WordOfDay word = AppDatabase.getInstance(appContext).wordOfDayDao()
                    .find(LocalDate.now().toString(), language);
            mainHandler.post(() -> callback.onResult(word));
        });
    }
}