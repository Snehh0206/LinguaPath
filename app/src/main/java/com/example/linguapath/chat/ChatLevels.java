package com.example.linguapath.chat;

import com.example.linguapath.R;

// Level names for the chat score
public class ChatLevels {

    private static final int[] THRESHOLDS = {0, 20, 60, 150, 300};
    private static final int[] NAMES = {
            R.string.level_0, R.string.level_1, R.string.level_2, R.string.level_3, R.string.level_4};

    public static int nameFor(int points) {
        int level = 0;
        for (int i = 0; i < THRESHOLDS.length; i++) {
            if (points >= THRESHOLDS[i]) {
                level = i;
            }
        }
        return NAMES[level];
    }
}