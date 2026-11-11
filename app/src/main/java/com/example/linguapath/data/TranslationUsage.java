package com.example.linguapath.data;

import android.content.Context;
import android.content.SharedPreferences;

import java.time.LocalDate;

// Our own count of the characters used today, so we stop before MyMemory does
public class TranslationUsage {

    // MyMemory allows about 5,000 characters a day without an email.
    // We stop earlier, at 4,500, so we never reach the real limit.
    public static final int DAILY_CHAR_LIMIT = 4500;
    public static final int WARN_REMAINING = 300; // warn when this few characters are left

    private final SharedPreferences prefs;

    public TranslationUsage(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences("linguapath_translation", Context.MODE_PRIVATE);
    }

    private static String today() {
        return LocalDate.now().toString();
    }

    public int getUsedToday() {
        return today().equals(prefs.getString("date", "")) ? prefs.getInt("used", 0) : 0;
    }

    public int getRemaining() {
        return Math.max(0, DAILY_CHAR_LIMIT - getUsedToday());
    }

    public void add(int chars) {
        prefs.edit().putString("date", today()).putInt("used", getUsedToday() + chars).apply();
    }

    // After the limit is hit, nothing new can be translated for the rest of the day
    public boolean isBlockedToday() {
        return today().equals(prefs.getString("blocked_date", ""));
    }

    public void blockForToday() {
        prefs.edit().putString("blocked_date", today()).apply();
    }

    public boolean canUse(int chars) {
        return !isBlockedToday() && getUsedToday() + chars <= DAILY_CHAR_LIMIT;
    }

    // True once per day, when the limit is close
    public boolean shouldWarnNow() {
        if (isBlockedToday() || getRemaining() > WARN_REMAINING) {
            return false;
        }
        if (today().equals(prefs.getString("warned_date", ""))) {
            return false;
        }
        prefs.edit().putString("warned_date", today()).apply();
        return true;
    }
}