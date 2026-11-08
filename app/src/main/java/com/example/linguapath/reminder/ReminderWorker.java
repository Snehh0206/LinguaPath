package com.example.linguapath.reminder;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.linguapath.data.ProgressStore;
import com.example.linguapath.data.ReminderSettings;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

// Runs in the background when a reminder is due. It only shows a notification when everything checks out.
public class ReminderWorker extends Worker {

    private static final String TAG = "ReminderWorker";
    private static final long TOO_LATE_MS = 3 * 60 * 60 * 1000L; // 3 hours
    private static final long STUDIED_MS = 60 * 1000L;           // one minute counts as studying
    private static final long EARLY_TOLERANCE_MS = 30 * 1000L;

    public ReminderWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        ReminderSettings settings = new ReminderSettings(context);

        long id = getInputData().getLong(ReminderScheduler.KEY_REMINDER_ID, 0);
        long target = getInputData().getLong(ReminderScheduler.KEY_TARGET_TIME, 0);
        boolean snooze = getInputData().getBoolean(ReminderScheduler.KEY_SNOOZE, false);

        if (!settings.isEnabled() || id == 0) {
            return Result.success();
        }
        return snooze
                ? runSnooze(context, settings, id)
                : runDaily(context, settings, id, target);
    }

    private Result runDaily(Context context, ReminderSettings settings, long id, long target) {
        long now = System.currentTimeMillis();

        // 1. Not the planned reminder any more (the user changed it, or this is a duplicate): do nothing
        if (settings.getScheduledId() != id) {
            Log.d(TAG, "Ignoring old reminder " + id);
            return Result.success();
        }
        // 2. Too early: the system ran us before the time. Do not notify. Plan the same time again.
        if (now < target - EARLY_TOLERANCE_MS) {
            Log.d(TAG, "Too early for reminder " + id + ", planning it again");
            ReminderScheduler.enqueueDaily(context, id, target);
            return Result.success();
        }
        // 3. Claim it. Only one job can win, so a duplicate can never show a second notification.
        if (!settings.claimScheduled(id)) {
            return Result.success();
        }
        // 4. The anti-spam rules
        if (isAllowed(context, now, target)) {
            Log.d(TAG, "Showing reminder " + id);
            NotificationHelper.showStudyReminder(context, id);
        }
        // 5. Always plan tomorrow's reminder
        ReminderScheduler.scheduleNextDay(context);
        return Result.success();
    }

    // "Remind me later" came back: only for the reminder that is live
    private Result runSnooze(Context context, ReminderSettings settings, long id) {
        if (!settings.isLive(id)) {
            Log.d(TAG, "Ignoring old snooze for reminder " + id);
            return Result.success();
        }
        if (isAllowed(context, System.currentTimeMillis(), 0)) {
            Log.d(TAG, "Showing snoozed reminder " + id);
            NotificationHelper.showStudyReminder(context, id);
        }
        return Result.success();
    }

    // Skip when: more than 3 hours late, nobody is logged in, or the user already studied today
    private boolean isAllowed(Context context, long now, long target) {
        if (target != 0 && now > target + TOO_LATE_MS) {
            return false;
        }
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return false;
        }
        return new ProgressStore(context, user.getUid()).getStudyMillisToday() < STUDIED_MS;
    }
}