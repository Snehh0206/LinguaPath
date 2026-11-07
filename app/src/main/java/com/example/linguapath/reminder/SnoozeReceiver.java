package com.example.linguapath.reminder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationManagerCompat;

import com.example.linguapath.data.ReminderSettings;

// Handles the "Remind me later" button on the notification.
// onReceive() stays short: it only does three quick things.
public class SnoozeReceiver extends BroadcastReceiver {

    public static final String ACTION_SNOOZE = "com.example.linguapath.ACTION_SNOOZE";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!ACTION_SNOOZE.equals(intent.getAction())) {
            return;
        }
        new ReminderSettings(context).addSnoozeToday();
        NotificationManagerCompat.from(context).cancel(NotificationHelper.NOTIFICATION_ID);
        ReminderScheduler.scheduleSnooze(context, ReminderScheduler.SNOOZE_MINUTES);
    }
}