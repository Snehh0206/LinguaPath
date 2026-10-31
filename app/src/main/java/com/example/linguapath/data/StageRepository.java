package com.example.linguapath.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.example.linguapath.model.Stage;

import java.util.ArrayList;
import java.util.List;

// The one place screens ask for stages and word counts.
// Work happens on a background thread, then the answer is handed back on the main thread.
public class StageRepository {

    public interface StagesCallback {
        void onStagesLoaded(List<Stage> stages);
    }

    public interface CountCallback {
        void onCountLoaded(int count);
    }

    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static void loadStages(Context context, String language, StagesCallback callback) {
        Context appContext = context.getApplicationContext();

        AppDatabase.databaseExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(appContext);
            DataImporter.importIfNeeded(appContext, db);

            List<StageEntity> entities = db.contentDao().getStagesForLanguage(language);
            List<Stage> stages = new ArrayList<>();
            for (StageEntity e : entities) {
                stages.add(new Stage(e.id, e.orderIndex, e.title, e.level));
            }

            mainHandler.post(() -> callback.onStagesLoaded(stages));
        });
    }

    public static void loadWordCount(Context context, int stageId, CountCallback callback) {
        Context appContext = context.getApplicationContext();

        AppDatabase.databaseExecutor.execute(() -> {
            int count = AppDatabase.getInstance(appContext).contentDao().countWordsInStage(stageId);
            mainHandler.post(() -> callback.onCountLoaded(count));
        });
    }
}