package com.example.linguapath.data;

import android.content.Context;
import android.content.SharedPreferences;

// Remembers the reminder choices on this phone
public class ReminderSettings {

    private final SharedPreferences prefs;

    public ReminderSettings(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences("linguapath_reminder", Context.MODE_PRIVATE);
    }

    public boolean isEnabled() {
        return prefs.getBoolean("enabled", false);
    }

    public void setEnabled(boolean enabled) {
        prefs.edit().putBoolean("enabled", enabled).apply();
    }

    public int getHour() {
        return prefs.getInt("hour", 19);
    }

    public int getMinute() {
        return prefs.getInt("minute", 0);
    }

    public void setTime(int hour, int minute) {
        prefs.edit().putInt("hour", hour).putInt("minute", minute).apply();
    }

    // The language studied last: tapping the reminder opens that path
    public String getLastLanguage() {
        return prefs.getString("last_language", null);
    }

    public void setLastLanguage(String language) {
        prefs.edit().putString("last_language", language).apply();
    }
}