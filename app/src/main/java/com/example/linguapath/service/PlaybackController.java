package com.example.linguapath.service;

import android.content.Context;
import android.content.Intent;

import androidx.core.content.ContextCompat;

// The only class screens use to control the playback
public class PlaybackController {

    // lesson = false: play all pronunciations. lesson = true: the audio lesson.
    public static void start(Context context, boolean lesson, String language,
                             int stageId, int stageIndex, String stageTitle) {
        Intent intent = new Intent(context, PlaybackService.class)
                .setAction(PlaybackService.ACTION_START)
                .putExtra(PlaybackService.EXTRA_LESSON, lesson)
                .putExtra(PlaybackService.EXTRA_LANGUAGE, language)
                .putExtra(PlaybackService.EXTRA_STAGE_ID, stageId)
                .putExtra(PlaybackService.EXTRA_STAGE_INDEX, stageIndex)
                .putExtra(PlaybackService.EXTRA_STAGE_TITLE, stageTitle);
        ContextCompat.startForegroundService(context, intent);
    }

    public static void toggle() {
        PlaybackService.toggleNow();
    }

    public static void stop() {
        PlaybackService.stopNow();
    }
}