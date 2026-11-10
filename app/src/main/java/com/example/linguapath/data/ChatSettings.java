package com.example.linguapath.data;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

public class ChatSettings {

    private final SharedPreferences prefs;

    public ChatSettings(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences("linguapath_chat", Context.MODE_PRIVATE);
    }

    public String getNickname(String uid) {
        return prefs.getString("nickname_" + uid, null);
    }

    public void setNickname(String uid, String name) {
        prefs.edit().putString("nickname_" + uid, name).apply();
    }

    public boolean isMuted(String uid) {
        return prefs.getStringSet("muted", new HashSet<>()).contains(uid);
    }

    public void mute(String uid) {
        // Copy the set first: Android says never to edit the one it gives you
        Set<String> copy = new HashSet<>(prefs.getStringSet("muted", new HashSet<>()));
        copy.add(uid);
        prefs.edit().putStringSet("muted", copy).apply();
    }

    // Auto points are capped per day, so nobody can farm the score
    public static final int DAILY_POINT_CAP = 30;

    private static String today() {
        return java.time.LocalDate.now().toString();
    }

    public int getPointsToday() {
        return today().equals(prefs.getString("points_date", ""))
                ? prefs.getInt("points_today", 0) : 0;
    }

    public void addPointsToday(int points) {
        prefs.edit()
                .putString("points_date", today())
                .putInt("points_today", getPointsToday() + points)
                .apply();
    }
}