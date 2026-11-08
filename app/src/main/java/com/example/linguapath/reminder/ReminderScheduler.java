package com.example.linguapath.reminder;

import android.content.Context;
import android.util.Log;

import androidx.core.app.NotificationManagerCompat;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.linguapath.data.ReminderSettings;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

// Plans the reminder with WorkManager (no exact alarms).
// Every reminder job carries the tag "study_reminder", so they can all be cancelled together.
public class ReminderScheduler {

    private static final String TAG = "ReminderScheduler";
    private static final String TAG_REMINDER = "study_reminder";
    private static final String TAG_SNOOZE = "study_reminder_snooze";
    private static final long MIN_LEAD_MS = 5000; // a reminder is always at least 5 seconds in the future

    public static final String KEY_REMINDER_ID = "reminder_id";
    public static final String KEY_TARGET_TIME = "target_time";
    public static final String KEY_SNOOZE = "snooze";
    public static final int SNOOZE_MINUTES = 3;

    // Called when the USER sets, changes or enables the reminder.
    // Removes every older job first, so exactly one reminder is planned.
    public static void scheduleDaily(Context context) {
        ReminderSettings settings = new ReminderSettings(context);
        cancelJobs(context);
        if (!settings.isEnabled()) {
            settings.clearReminders();
            return;
        }
        settings.resetLive(); // old "Remind me later" jobs and buttons stop working
        plan(context, settings, nextOccurrence(settings, System.currentTimeMillis()));
    }

    // Called by the worker after a reminder was shown or skipped: plans tomorrow's reminder
    static void scheduleNextDay(Context context) {
        ReminderSettings settings = new ReminderSettings(context);
        if (settings.isEnabled()) {
            plan(context, settings, nextOccurrence(settings, System.currentTimeMillis()));
        }
    }

    private static void plan(Context context, ReminderSettings settings, long target) {
        settings.setScheduledId(target); // the reminder's id is its time
        enqueueDaily(context, target, target);
    }

    // The next time of day that is still in the future (today if it has not passed, otherwise tomorrow)
    private static long nextOccurrence(ReminderSettings settings, long now) {
        Calendar next = Calendar.getInstance();
        next.setTimeInMillis(now);
        next.set(Calendar.HOUR_OF_DAY, settings.getHour());
        next.set(Calendar.MINUTE, settings.getMinute());
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);
        while (next.getTimeInMillis() <= now + MIN_LEAD_MS) {
            next.add(Calendar.DAY_OF_YEAR, 1);
        }
        return next.getTimeInMillis();
    }

    static void enqueueDaily(Context context, long reminderId, long target) {
        long delay = Math.max(0, target - System.currentTimeMillis());
        Data data = new Data.Builder()
                .putLong(KEY_REMINDER_ID, reminderId)
                .putLong(KEY_TARGET_TIME, target)
                .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(ReminderWorker.class)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(data)
                .addTag(TAG_REMINDER)
                .build();
        WorkManager.getInstance(context).enqueue(request);
        Log.d(TAG, "Planned reminder " + reminderId + " in " + (delay / 1000) + " s");
    }

    // A one-time reminder after "Remind me later". It belongs to one reminder (reminderId).
    public static void scheduleSnooze(Context context, long reminderId, int minutes) {
        WorkManager manager = WorkManager.getInstance(context);
        manager.cancelAllWorkByTag(TAG_SNOOZE); // only one snooze waits at a time

        Data data = new Data.Builder()
                .putLong(KEY_REMINDER_ID, reminderId)
                .putBoolean(KEY_SNOOZE, true)
                .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(ReminderWorker.class)
                .setInitialDelay(minutes, TimeUnit.MINUTES)
                .setInputData(data)
                .addTag(TAG_REMINDER)
                .addTag(TAG_SNOOZE)
                .build();
        manager.enqueue(request);
        Log.d(TAG, "Snooze for reminder " + reminderId + " in " + minutes + " min");
    }

    private static void cancelJobs(Context context) {
        WorkManager.getInstance(context).cancelAllWorkByTag(TAG_REMINDER);
    }

    // The reminder was switched off: remove every job, forget the reminders, dismiss the notification
    public static void cancelAll(Context context) {
        cancelJobs(context);
        new ReminderSettings(context).clearReminders();
        NotificationManagerCompat.from(context).cancel(NotificationHelper.NOTIFICATION_ID);
    }

    // The test button: shows a reminder now. It is a real reminder with its own 3 snoozes,
    // and it does not change the planned daily reminder.
    public static void fireTest(Context context) {
        long id = System.currentTimeMillis();
        new ReminderSettings(context).startLive(id);
        NotificationHelper.showStudyReminder(context, id);
    }
}