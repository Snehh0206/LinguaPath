package com.example.linguapath.reminder;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.linguapath.data.ProgressStore;
import com.example.linguapath.data.ReminderSettings;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

// Runs in the background when the reminder time arrives, and only nudges when it makes sense
public class ReminderWorker extends Worker {

    private static final long TOO_LATE_MS = 3 * 60 * 60 * 1000L; // 3 hours
    private static final long STUDIED_MS = 60 * 1000L;           // one minute counts as studying

    public ReminderWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();

        if (!new ReminderSettings(context).isEnabled()) {
            return Result.success();
        }

        boolean snooze = getInputData().getBoolean(ReminderScheduler.KEY_SNOOZE, false);
        long target = getInputData().getLong(ReminderScheduler.KEY_TARGET_TIME, 0);

        // Rule 1: the phone was off or asleep for hours? Skip it, never nag late.
        boolean tooLate = target != 0 && System.currentTimeMillis() > target + TOO_LATE_MS;

        // Rule 2: nobody is logged in? Skip.
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        // Rule 3: already studied today? Skip.
        boolean alreadyStudied = user != null
                && new ProgressStore(context, user.getUid()).getStudyMillisToday() >= STUDIED_MS;

        if (!tooLate && user != null && !alreadyStudied) {
            NotificationHelper.showStudyReminder(context);
        }

        if (!snooze) {
            ReminderScheduler.scheduleDaily(context); // always plan tomorrow's reminder
        }
        return Result.success();
    }
}