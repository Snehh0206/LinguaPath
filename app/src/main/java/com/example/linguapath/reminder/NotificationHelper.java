package com.example.linguapath.reminder;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
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

    // One notification slot on purpose: a new reminder replaces the old one instead of stacking up
    public static final int NOTIFICATION_ID = 1001;

    private static final String TAG = "NotificationHelper";

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
    public static void showStudyReminder(Context context, long reminderId) {
        createChannel(context);

        NotificationManagerCompat manager = NotificationManagerCompat.from(context);
        if (!manager.areNotificationsEnabled()) {
            Log.d(TAG, "Notifications are off, nothing to show");
            return; // the app works fine without them
        }

        ReminderSettings settings = new ReminderSettings(context);
        String language = settings.getLastLanguage();

        // Tapping opens the language path the user studied last, or the language screen
        Intent open;
        if (language != null) {
            open = new Intent(context, LanguageDashboardActivity.class);
            open.putExtra(LanguageDashboardActivity.EXTRA_LANGUAGE, language);
        } else {
            open = new Intent(context, LanguageSelectActivity.class);
        }
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        open.setData(Uri.parse("linguapath://open/" + reminderId)); // a different address for every reminder

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

        // "Remind me later" shows on this reminder's notification until its own snoozes are used up
        if (settings.canSnooze(reminderId)) {
            Intent snooze = new Intent(context, SnoozeReceiver.class);
            snooze.setAction(SnoozeReceiver.ACTION_SNOOZE);
            snooze.setData(Uri.parse("linguapath://snooze/" + reminderId)); // this button belongs to this reminder only
            snooze.putExtra(SnoozeReceiver.EXTRA_REMINDER_ID, reminderId);

            PendingIntent snoozePending = PendingIntent.getBroadcast(context, 0, snooze,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            builder.addAction(R.drawable.ic_notification,
                    context.getString(R.string.reminder_snooze), snoozePending);
        }

        try {
            manager.notify(NOTIFICATION_ID, builder.build());
        } catch (SecurityException e) {
            Log.w(TAG, "Could not show the notification: " + e.getMessage());
        }
    }
}