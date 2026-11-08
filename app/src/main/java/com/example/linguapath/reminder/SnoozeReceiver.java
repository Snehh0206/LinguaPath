package com.example.linguapath.reminder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationManagerCompat;

import com.example.linguapath.data.ReminderSettings;

// Handles the "Remind me later" button on the notification.
// onReceive() stays short: check, count, plan, dismiss.
public class SnoozeReceiver extends BroadcastReceiver {

    public static final String ACTION_SNOOZE = "com.example.linguapath.ACTION_SNOOZE";
    public static final String EXTRA_REMINDER_ID = "extra_reminder_id";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!ACTION_SNOOZE.equals(intent.getAction())) {
            return;
        }
        long reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, 0);

        // Counts one snooze for THIS reminder. Refused for an old reminder, or when none are left.
        if (new ReminderSettings(context).useSnooze(reminderId)) {
            ReminderScheduler.scheduleSnooze(context, reminderId, ReminderScheduler.SNOOZE_MINUTES);
        }
        NotificationManagerCompat.from(context).cancel(NotificationHelper.NOTIFICATION_ID);
    }
}