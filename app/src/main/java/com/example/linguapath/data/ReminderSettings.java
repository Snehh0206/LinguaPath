package com.example.linguapath.data;

import android.content.Context;
import android.content.SharedPreferences;

// Remembers the reminder choices on this phone.
// A reminder's id is the time it is planned for, in milliseconds.
//   scheduled id = the reminder planned for the future (0 = none)
//   live id      = the reminder that was last shown. Its "Remind me later" counter belongs to it.
public class ReminderSettings {

    // Each reminder can be snoozed this many times. Change the number here if you want more.
    public static final int MAX_SNOOZES_PER_REMINDER = 3;

    private static final Object LOCK = new Object();

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

    // ---- Planned reminder ----

    public long getScheduledId() {
        return prefs.getLong("scheduled_id", 0);
    }

    public void setScheduledId(long id) {
        synchronized (LOCK) {
            prefs.edit().putLong("scheduled_id", id).apply();
        }
    }

    // The planned reminder is shown now. Only the first caller for this id wins,
    // so a duplicate job can never show a second notification.
    // The reminder becomes "live" and gets a fresh snooze counter.
    public boolean claimScheduled(long id) {
        synchronized (LOCK) {
            if (id == 0 || prefs.getLong("scheduled_id", 0) != id) {
                return false;
            }
            prefs.edit()
                    .putLong("scheduled_id", 0)
                    .putLong("live_id", id)
                    .putInt("snooze_count", 0)
                    .apply();
            return true;
        }
    }

    // ---- Live reminder and its snoozes ----

    // Used by the test button: a test is a real reminder with its own snooze counter
    public void startLive(long id) {
        synchronized (LOCK) {
            prefs.edit().putLong("live_id", id).putInt("snooze_count", 0).apply();
        }
    }

    // Old snooze jobs and buttons stop working
    public void resetLive() {
        synchronized (LOCK) {
            prefs.edit().putLong("live_id", 0).putInt("snooze_count", 0).apply();
        }
    }

    public void clearReminders() {
        synchronized (LOCK) {
            prefs.edit()
                    .putLong("scheduled_id", 0)
                    .putLong("live_id", 0)
                    .putInt("snooze_count", 0)
                    .apply();
        }
    }

    public boolean isLive(long id) {
        return id != 0 && prefs.getLong("live_id", 0) == id;
    }

    // Should this reminder's notification show the "Remind me later" button?
    public boolean canSnooze(long id) {
        return isLive(id) && prefs.getInt("snooze_count", 0) < MAX_SNOOZES_PER_REMINDER;
    }

    // The user tapped "Remind me later". Counts it. false = not allowed (old reminder, or no snoozes left).
    public boolean useSnooze(long id) {
        synchronized (LOCK) {
            if (!isLive(id)) {
                return false;
            }
            int count = prefs.getInt("snooze_count", 0);
            if (count >= MAX_SNOOZES_PER_REMINDER) {
                return false;
            }
            prefs.edit().putInt("snooze_count", count + 1).apply();
            return true;
        }
    }
}