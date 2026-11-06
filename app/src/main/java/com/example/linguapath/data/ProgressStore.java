package com.example.linguapath.data;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

// Remembers how many stages are finished, per language.
public class ProgressStore {

    private static final String PREFS_NAME = "linguapath_progress";

    private final SharedPreferences prefs;

    public ProgressStore(Context context) {
        // Each account gets its own notebook, so two users never share progress
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String userId = (user == null) ? "guest" : user.getUid();
        prefs = context.getSharedPreferences("linguapath_progress_" + userId, Context.MODE_PRIVATE);
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

    // Used when the cloud copy is ahead of this phone. Progress only goes forward.
    public boolean raiseCompletedTo(String language, int value) {
        if (value > getCompletedCount(language)) {
            prefs.edit().putInt("completed_" + language, value).apply();
            return true;
        }
        return false;
    }

    // How many times a finished stage was redone (no extra progress).
    // Saved now so revision suggestions can use it later.
    public int getPracticeCount(String language, int stageIndex) {
        return prefs.getInt("practice_" + language + "_" + stageIndex, 0);
    }

    public void addPractice(String language, int stageIndex) {
        int count = getPracticeCount(language, stageIndex) + 1;
        prefs.edit().putInt("practice_" + language + "_" + stageIndex, count).apply();
    }
}