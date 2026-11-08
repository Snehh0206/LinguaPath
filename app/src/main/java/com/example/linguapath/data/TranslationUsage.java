package com.example.linguapath.data;

import android.content.Context;
import android.content.SharedPreferences;

import java.time.LocalDate;

// Our own estimate of the characters used today, so we stop before MyMemory does
public class TranslationUsage {

    public static final int DAILY_CHAR_LIMIT = 5000;

    private final SharedPreferences prefs;

    public TranslationUsage(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences("linguapath_translation", Context.MODE_PRIVATE);
    }

    public int getUsedToday() {
        String today = LocalDate.now().toString();
        return today.equals(prefs.getString("date", "")) ? prefs.getInt("used", 0) : 0;
    }

    public void add(int chars) {
        prefs.edit()
                .putString("date", LocalDate.now().toString())
                .putInt("used", getUsedToday() + chars)
                .apply();
    }

    public boolean canUse(int chars) {
        return getUsedToday() + chars <= DAILY_CHAR_LIMIT;
    }
}