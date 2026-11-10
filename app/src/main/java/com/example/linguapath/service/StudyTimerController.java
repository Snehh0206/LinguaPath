package com.example.linguapath.service;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import androidx.core.content.ContextCompat;

// The only class screens use to control the study timer
public class StudyTimerController {

    // true: the timer pauses when you leave the app, and resumes when you come back.
    // false: it keeps running in the background, with its notification.
    public static final boolean PAUSE_WHEN_APP_LEAVES = true;

    private static final long LEAVE_DELAY_MS = 2000; // a short wait, so rotating the phone does not pause it

    private static final Handler handler = new Handler(Looper.getMainLooper());
    private static final Runnable pauseWhenAway = StudyTimerService::autoPause;
    private static int startedScreens = 0;

    // Call from onStart / onStop of every study screen (lesson page and word page)
    public static void screenStarted() {
        startedScreens++;
        handler.removeCallbacks(pauseWhenAway);
        StudyTimerService.autoResume();
    }

    public static void screenStopped() {
        if (startedScreens > 0) {
            startedScreens--;
        }
        if (startedScreens == 0 && PAUSE_WHEN_APP_LEAVES) {
            handler.postDelayed(pauseWhenAway, LEAVE_DELAY_MS);
        }
    }

    // Opening a stage starts its timer by itself
    public static void begin(Context context, String language, int stageId, int stageIndex, String stageTitle) {
        String key = language + "#" + stageIndex;
        if (key.equals(StudyTimerState.stoppedKey)) {
            return; // the user pressed Stop for this stage
        }
        StudyTimerState.Snapshot s = StudyTimerState.live().getValue();
        if (s != null && s.active && key.equals(s.language + "#" + s.stageIndex)) {
            return; // already timing this stage (for example after a rotation)
        }
        Intent intent = new Intent(context, StudyTimerService.class)
                .setAction(StudyTimerService.ACTION_START)
                .putExtra(StudyTimerService.EXTRA_LANGUAGE, language)
                .putExtra(StudyTimerService.EXTRA_STAGE_ID, stageId)
                .putExtra(StudyTimerService.EXTRA_STAGE_INDEX, stageIndex)
                .putExtra(StudyTimerService.EXTRA_STAGE_TITLE, stageTitle);
        ContextCompat.startForegroundService(context, intent);
    }

    public static void togglePause() {
        StudyTimerService.toggleManual();
    }

    // The stage was finished or left: save the final time and stop the service
    public static void endSession() {
        StudyTimerState.stoppedKey = null;
        StudyTimerService.endNow();
    }
}