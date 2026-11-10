package com.example.linguapath.data;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Translations that could not be done because the phone was offline. They are retried later.
public class PendingTranslations {

    private static final String PREFS = "linguapath_pending";
    private static final String KEY = "translations";
    private static final int MAX = 20;
    private static final char SEPARATOR = '\u0001';

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static synchronized void add(Context context, String pair, String text) {
        SharedPreferences prefs = prefs(context);
        Set<String> set = new HashSet<>(prefs.getStringSet(KEY, new HashSet<>()));
        if (set.size() >= MAX) {
            return;
        }
        set.add(pair + SEPARATOR + text);
        prefs.edit().putStringSet(KEY, set).apply();
    }

    // Returns every waiting translation as {pair, text} and clears the list
    public static synchronized List<String[]> takeAll(Context context) {
        SharedPreferences prefs = prefs(context);
        List<String[]> items = new ArrayList<>();
        for (String entry : prefs.getStringSet(KEY, new HashSet<>())) {
            int cut = entry.indexOf(SEPARATOR);
            if (cut > 0) {
                items.add(new String[]{entry.substring(0, cut), entry.substring(cut + 1)});
            }
        }
        prefs.edit().remove(KEY).apply();
        return items;
    }
}