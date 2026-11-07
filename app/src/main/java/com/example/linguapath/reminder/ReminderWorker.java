package com.example.linguapath.reminder;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.linguapath.data.ReminderSettings;

// Runs in the background when the reminder time arrives
public class ReminderWorker extends Worker {

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

        NotificationHelper.showStudyReminder(context);
        ReminderScheduler.scheduleDaily(context); // plan tomorrow's reminder
        return Result.success();
    }
}