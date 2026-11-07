package com.example.linguapath.reminder;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.linguapath.LanguageDashboardActivity;
import com.example.linguapath.LanguageSelectActivity;
import com.example.linguapath.R;
import com.example.linguapath.data.ReminderSettings;

// Builds and shows the study reminder notification
public class NotificationHelper {

    public static final String CHANNEL_ID = "study_reminders";
    public static final int NOTIFICATION_ID = 1001;
    private static final String TAG = "NotificationHelper";
    private static final int MAX_SNOOZES_PER_DAY = 3;


    // A channel is the "category" the user can switch on or off in system Settings.
    // Creating it again is harmless.
    public static void createChannel(Context context) {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription(context.getString(R.string.channel_description));
        context.getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }

    @SuppressLint("MissingPermission") // we check areNotificationsEnabled() right below
    public static void showStudyReminder(Context context) {
        createChannel(context);

        NotificationManagerCompat manager = NotificationManagerCompat.from(context);
        if (!manager.areNotificationsEnabled()) {
            Log.d(TAG, "Notifications are off, nothing to show");
            return; // the app works fine without them
        }

        String language = new ReminderSettings(context).getLastLanguage();

        // Tapping opens the language path the user studied last, or the language screen
        Intent open;
        if (language != null) {
            open = new Intent(context, LanguageDashboardActivity.class);
            open.putExtra(LanguageDashboardActivity.EXTRA_LANGUAGE, language);
        } else {
            open = new Intent(context, LanguageSelectActivity.class);
        }
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent openPending = PendingIntent.getActivity(context, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String text = (language == null)
                ? context.getString(R.string.reminder_text_generic)
                : context.getString(R.string.reminder_text_language, language);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(ContextCompat.getColor(context, R.color.primary))
                .setContentTitle(context.getString(R.string.reminder_title))
                .setContentText(text)
                .setContentIntent(openPending)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        // "Remind me later" button, shown only up to 3 times a day
        if (new ReminderSettings(context).getSnoozeCountToday() < MAX_SNOOZES_PER_DAY) {
            Intent snooze = new Intent(context, SnoozeReceiver.class);
            snooze.setAction(SnoozeReceiver.ACTION_SNOOZE);
            PendingIntent snoozePending = PendingIntent.getBroadcast(context, 1, snooze,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            builder.addAction(0, context.getString(R.string.reminder_snooze), snoozePending);
        }
        manager.notify(NOTIFICATION_ID, builder.build());
    }
}