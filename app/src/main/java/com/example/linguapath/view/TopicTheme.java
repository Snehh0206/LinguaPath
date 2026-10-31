package com.example.linguapath.view;

import com.example.linguapath.R;

// Picks a sticker and a header color for a topic.
// Later, this is the one file to change when real illustrations arrive.
public class TopicTheme {

    public final String sticker;
    public final int backgroundRes;

    private TopicTheme(String sticker, int backgroundRes) {
        this.sticker = sticker;
        this.backgroundRes = backgroundRes;
    }

    public static TopicTheme forTitle(String title) {
        String t = (title == null) ? "" : title.toLowerCase();

        if (t.contains("greeting")) {
            return new TopicTheme("👋", R.drawable.bg_card_peach);
        } else if (t.contains("food")) {
            return new TopicTheme("🍞", R.drawable.bg_card_butter);
        } else if (t.contains("travel")) {
            return new TopicTheme("✈️", R.drawable.bg_card_sky);
        } else if (t.contains("number")) {
            return new TopicTheme("🔢", R.drawable.bg_card_sage);
        } else if (t.contains("daily")) {
            return new TopicTheme("🏡", R.drawable.bg_card_butter);
        } else if (t.contains("hangul")) {
            return new TopicTheme("🌱", R.drawable.bg_card_sage);
        }
        return new TopicTheme("🌿", R.drawable.bg_card_sage);
    }
}