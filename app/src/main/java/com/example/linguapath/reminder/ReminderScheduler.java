package com.example.linguapath.reminder;

import android.content.Context;

import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.linguapath.data.ReminderSettings;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

// Plans the reminder with WorkManager (no exact alarms needed).
// Each reminder plans tomorrow's one when it runs.
public class ReminderScheduler {

    private static final String DAILY_WORK = "daily_study_reminder";
    public static final String KEY_TARGET_TIME = "target_time";
    public static final String KEY_SNOOZE = "snooze";

    // Plans the next reminder at the chosen time (today if still ahead, otherwise tomorrow)
    public static void scheduleDaily(Context context) {
        ReminderSettings settings = new ReminderSettings(context);
        if (!settings.isEnabled()) {
            cancelAll(context);
            return;
        }

        Calendar next = Calendar.getInstance();
        next.set(Calendar.HOUR_OF_DAY, settings.getHour());
        next.set(Calendar.MINUTE, settings.getMinute());
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);
        if (next.getTimeInMillis() <= System.currentTimeMillis()) {
            next.add(Calendar.DAY_OF_YEAR, 1);
        }

        long delay = next.getTimeInMillis() - System.currentTimeMillis();
        Data data = new Data.Builder()
                .putLong(KEY_TARGET_TIME, next.getTimeInMillis())
                .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(ReminderWorker.class)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(data)
                .build();

        // REPLACE: changing the time never leaves two reminders planned
        WorkManager.getInstance(context)
                .enqueueUniqueWork(DAILY_WORK, ExistingWorkPolicy.REPLACE, request);
    }

    public static void cancelAll(Context context) {
        WorkManager.getInstance(context).cancelUniqueWork(DAILY_WORK);
    }
}