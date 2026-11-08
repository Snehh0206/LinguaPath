package com.example.linguapath;

import android.Manifest;
import android.app.TimePickerDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.format.DateFormat;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;

import com.example.linguapath.data.ReminderSettings;
import com.example.linguapath.databinding.ActivityReminderBinding;
import com.example.linguapath.reminder.NotificationHelper;
import com.example.linguapath.reminder.ReminderScheduler;

import java.util.Locale;

public class ReminderActivity extends AppCompatActivity {

    private ActivityReminderBinding binding;
    private ReminderSettings settings;
    private ActivityResultLauncher<String> permissionLauncher;
    private boolean updatingSwitch = false; // true while WE change the switch, not the user

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityReminderBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        settings = new ReminderSettings(this);
        NotificationHelper.createChannel(this);

        // The permission request must be registered before the screen is shown
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), this::onPermissionResult);

        binding.buttonBack.setOnClickListener(v -> finish());
        binding.textTime.setOnClickListener(v -> pickTime());
        binding.buttonTest.setOnClickListener(v -> sendTest());
        binding.switchReminder.setOnCheckedChangeListener((button, checked) -> {
            if (!updatingSwitch) {
                onSwitchChanged(checked);
            }
        });

        binding.textTime.setText(formatTime());
        boolean on = settings.isEnabled() && notificationsAllowed();
        setSwitch(on);
        binding.textStatus.setText(on
                ? getString(R.string.reminder_status_on, formatTime())
                : getString(R.string.reminder_status_off));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // The user may have turned notifications off in system Settings
        if (settings.isEnabled() && !notificationsAllowed()) {
            settings.setEnabled(false);
            ReminderScheduler.cancelAll(this);
            setSwitch(false);
            binding.textStatus.setText(R.string.reminder_revoked);
        }
    }

    // ---- Switch and permission flow ----

    private void onSwitchChanged(boolean checked) {
        if (!checked) {
            disableReminder();
        } else if (notificationsAllowed()) {
            enableReminder();
        } else {
            explainThenAsk(); // ask only now, when the feature needs it, and say why
        }
    }

    private boolean notificationsAllowed() {
        return NotificationManagerCompat.from(this).areNotificationsEnabled();
    }

    private void explainThenAsk() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.reminder_why_title)
                .setMessage(R.string.reminder_why_message)
                .setPositiveButton(R.string.reminder_why_continue, (dialog, which) -> askPermission())
                .setNegativeButton(R.string.reminder_not_now, (dialog, which) -> declineSoftly())
                .setOnCancelListener(dialog -> declineSoftly())
                .show();
    }

    private void askPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        } else {
            // Before Android 13 there is no popup. Notifications were switched off in Settings.
            setSwitch(false);
            showSettingsDialog();
        }
    }

    private void onPermissionResult(boolean granted) {
        if (granted) {
            enableReminder();
            return;
        }
        setSwitch(false);
        if (!shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
            // "Don't ask again": Android will not show the popup any more. Only Settings can help.
            showSettingsDialog();
        } else {
            binding.textStatus.setText(R.string.reminder_denied_soft);
        }
    }

    private void declineSoftly() {
        setSwitch(false);
        binding.textStatus.setText(R.string.reminder_denied_soft);
    }

    private void showSettingsDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.reminder_settings_title)
                .setMessage(R.string.reminder_settings_message)
                .setPositiveButton(R.string.reminder_open_settings, (dialog, which) -> openNotificationSettings())
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void openNotificationSettings() {
        Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            binding.textStatus.setText(R.string.reminder_settings_message);
        }
    }

    // ---- Turning the reminder on and off ----

    private void enableReminder() {
        settings.setEnabled(true);
        ReminderScheduler.scheduleDaily(this);
        setSwitch(true);
        binding.textStatus.setText(getString(R.string.reminder_status_on, formatTime()));
    }

    private void disableReminder() {
        settings.setEnabled(false);
        ReminderScheduler.cancelAll(this);
        binding.textStatus.setText(R.string.reminder_status_off);
    }

    private void setSwitch(boolean on) {
        updatingSwitch = true;
        binding.switchReminder.setChecked(on);
        updatingSwitch = false;
    }

    // ---- Time and test ----

    private void pickTime() {
        boolean is24 = DateFormat.is24HourFormat(this);
        new TimePickerDialog(this, (view, hour, minute) -> {
            settings.setTime(hour, minute);
            binding.textTime.setText(formatTime());
            if (settings.isEnabled()) {
                ReminderScheduler.scheduleDaily(this); // move the planned reminder
                binding.textStatus.setText(getString(R.string.reminder_status_on, formatTime()));
            }
        }, settings.getHour(), settings.getMinute(), is24).show();
    }

    private void sendTest() {
        if (!notificationsAllowed()) {
            binding.textStatus.setText(R.string.reminder_test_blocked);
            return;
        }
        ReminderScheduler.fireTest(this); // a real reminder with its own 3 snoozes
        binding.textStatus.setText(R.string.reminder_test_sent);
    }

    private String formatTime() {
        return String.format(Locale.US, "%02d:%02d", settings.getHour(), settings.getMinute());
    }
}