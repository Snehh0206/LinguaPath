package com.example.linguapath.data;

import android.content.Context;
import android.content.SharedPreferences;

// Remembers how many stages are finished, per language.
public class ProgressStore {

    private static final String PREFS_NAME = "linguapath_progress";

    private final SharedPreferences prefs;

    public ProgressStore(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public int getCompletedCount(String language) {
        return prefs.getInt("completed_" + language, 0);
    }

    // Only the next stage in order can be completed
    public void markStageCompleted(String language, int stageIndex) {
        if (stageIndex == getCompletedCount(language)) {
            prefs.edit().putInt("completed_" + language, stageIndex + 1).apply();
        }
    }
}