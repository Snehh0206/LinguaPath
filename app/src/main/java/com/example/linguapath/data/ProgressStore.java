package com.example.linguapath.data;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Map;

// Remembers how many stages are finished, per language.
public class ProgressStore {

    private static final String PREFS_NAME = "linguapath_progress";

    private final SharedPreferences prefs;

    public ProgressStore(Context context) {
        this(context, currentUserId());
    }

    // Used by the study service, which knows who the session belongs to
    public ProgressStore(Context context, String userId) {
        prefs = context.getSharedPreferences("linguapath_progress_" + userId, Context.MODE_PRIVATE);
    }

    private static String currentUserId() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return (user == null) ? "guest" : user.getUid();
    }
    private static String today() {
        return java.time.LocalDate.now().toString(); // for example 2026-11-02
    }

    // Adds studied time to today's total
    public void addStudyMillis(long millis) {
        String key = "study_" + today();
        prefs.edit().putLong(key, prefs.getLong(key, 0) + millis).apply();
    }

    public long getStudyMillisToday() {
        return prefs.getLong("study_" + today(), 0);
    }
    public int getCompletedCount(String language) {
        return prefs.getInt("completed_" + language, 0);
    }
    // Study time for one stage (in addition to the daily total)
    public void addStageStudyMillis(String language, int stageIndex, long millis) {
        String key = "stagetime_" + language + "_" + stageIndex;
        prefs.edit().putLong(key, prefs.getLong(key, 0) + millis).apply();
    }

    public long getStageStudyMillis(String language, int stageIndex) {
        return prefs.getLong("stagetime_" + language + "_" + stageIndex, 0);
    }

    // True while a progress upload has not been confirmed yet
    public void setSyncPending(String language, boolean pending) {
        prefs.edit().putBoolean("sync_pending_" + language, pending).apply();
    }

    public boolean isSyncPending(String language) {
        return prefs.getBoolean("sync_pending_" + language, false);
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

    public static final int XP_PER_STAGE = 10;

    // All finished stages across every language
    public int getTotalCompleted() {
        int total = 0;
        for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
            if (entry.getKey().startsWith("completed_") && entry.getValue() instanceof Integer) {
                total += (Integer) entry.getValue();
            }
        }
        return total;
    }

    // XP only comes from finishing stages. Redoing a stage gives none.
    // 10 XP per finished stage, plus bonuses for perfect first tries
    public int getXp() {
        return getTotalCompleted() * XP_PER_STAGE + prefs.getInt("bonus_xp", 0);
    }

    public void addBonusXp(int xp) {
        prefs.edit().putInt("bonus_xp", prefs.getInt("bonus_xp", 0) + xp).apply();
    }
    // The days with at least one minute of study (for the streak)
    public java.util.List<String> getStudyDays() {
        java.util.List<String> days = new java.util.ArrayList<>();
        for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
            if (entry.getKey().startsWith("study_")
                    && entry.getValue() instanceof Long
                    && (Long) entry.getValue() >= 60000) {
                days.add(entry.getKey().substring("study_".length()));
            }
        }
        return days;
    }
}