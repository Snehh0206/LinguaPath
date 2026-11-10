package com.example.linguapath.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;
import androidx.core.content.ContextCompat;

import com.example.linguapath.CourseActivity;
import com.example.linguapath.LanguageDashboardActivity;
import com.example.linguapath.R;
import com.example.linguapath.data.ProgressStore;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Locale;

// A FOREGROUND service: it shows a notification, so the user always knows it is running.
// It times one stage at a time, and saves the time per stage and per day.
public class StudyTimerService extends Service {

    public static final String ACTION_START = "com.example.linguapath.TIMER_START";
    public static final String ACTION_TOGGLE = "com.example.linguapath.TIMER_TOGGLE";
    public static final String ACTION_STOP = "com.example.linguapath.TIMER_STOP";

    public static final String EXTRA_LANGUAGE = "extra_language";
    public static final String EXTRA_STAGE_ID = "extra_stage_id";
    public static final String EXTRA_STAGE_INDEX = "extra_stage_index";
    public static final String EXTRA_STAGE_TITLE = "extra_stage_title";

    private static final String TAG = "StudyTimerService";
    private static final String CHANNEL_ID = "study_timer";
    private static final int NOTIFICATION_ID = 2001;
    private static final long FLUSH_MS = 10_000; // save to the totals every 10 seconds

    private static StudyTimerService instance;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private String language;     // null = no session
    private String stageTitle = "";
    private String uid = "guest";
    private int stageId;
    private int stageIndex;
    private long sessionMs;
    private long unsavedMs;
    private long lastTick;
    private boolean running;
    private boolean manualPaused;
    private boolean autoPaused;

    // ---- Calls from the rest of the app (same process, no binding) ----

    public static boolean isRunning() {
        return instance != null;
    }

    static void autoPause() {
        if (instance != null) {
            instance.pauseInternal(false);
        }
    }

    static void autoResume() {
        if (instance != null) {
            instance.resumeInternal(false);
        }
    }

    static void toggleManual() {
        if (instance != null) {
            if (instance.running) {
                instance.pauseInternal(true);
            } else {
                instance.resumeInternal(true);
            }
        }
    }

    // The session ends (stage finished, Back, logout). The final time is saved.
    static void endNow() {
        if (instance != null) {
            instance.endSession(false);
        }
    }

    public static String format(long ms) {
        long total = ms / 1000;
        long h = total / 3600;
        long m = (total % 3600) / 60;
        long s = total % 60;
        return h > 0
                ? String.format(Locale.US, "%d h %02d min %02d s", h, m, s)
                : String.format(Locale.US, "%d min %02d s", m, s);
    }
    // Longer text with seconds, for example "12 min 34 s"
    public static String formatLong(long ms) {
        long total = ms / 1000;
        long h = total / 3600;
        long m = (total % 3600) / 60;
        long s = total % 60;
        return h > 0
                ? String.format(Locale.US, "%d h %02d min %02d s", h, m, s)
                : String.format(Locale.US, "%d min %02d s", m, s);
    }

    // ---- Lifecycle ----

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        Log.d(TAG, "onCreate");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null; // not a bound service
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = (intent == null) ? null : intent.getAction();
        Log.d(TAG, "onStartCommand " + action);

        if (ACTION_START.equals(action)) {
            startSession(intent);
        } else if (ACTION_TOGGLE.equals(action)) {
            if (running) {
                pauseInternal(true);
            } else {
                resumeInternal(true);
            }
        } else if (ACTION_STOP.equals(action)) {
            endSession(true); // the user pressed Stop in the notification
        } else if (language == null) {
            stopSelf(); // restarted with nothing to do
        }
        return START_NOT_STICKY;
    }

    // The user swiped the app away: end the session
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        endSession(false);
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        handler.removeCallbacksAndMessages(null);
        if (language != null) {
            addElapsed();
            flush();
            StudyTimerState.publish(StudyTimerState.IDLE);
        }
        instance = null;
        super.onDestroy();
    }

    // ---- Session ----

    private void startSession(Intent intent) {
        String newLanguage = intent.getStringExtra(EXTRA_LANGUAGE);
        int newIndex = intent.getIntExtra(EXTRA_STAGE_INDEX, 0);
        boolean sameStage = language != null && language.equals(newLanguage) && stageIndex == newIndex;

        if (!sameStage) {
            if (language != null) {
                addElapsed();
                flush(); // another stage was open: save its time first
            }
            language = (newLanguage == null) ? "" : newLanguage;
            stageId = intent.getIntExtra(EXTRA_STAGE_ID, 0);
            stageIndex = newIndex;
            String title = intent.getStringExtra(EXTRA_STAGE_TITLE);
            stageTitle = (title == null) ? "" : title;

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            uid = (user == null) ? "guest" : user.getUid();

            sessionMs = 0;
            unsavedMs = 0;
            manualPaused = false;
            autoPaused = false;
            running = true;
            lastTick = SystemClock.elapsedRealtime();
            StudyTimerState.stoppedKey = null;
        }

        // A foreground service must show its notification right away
        createChannel();
        ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);

        handler.removeCallbacks(tick);
        if (running) {
            handler.post(tick);
        } else {
            refresh();
        }
    }

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            addElapsed();
            if (unsavedMs >= FLUSH_MS) {
                flush();
            }
            refresh();
            handler.postDelayed(this, 1000);
        }
    };

    // Adds the time since the last tick, but only while the timer is running
    private void addElapsed() {
        long now = SystemClock.elapsedRealtime();
        if (running) {
            long delta = now - lastTick;
            sessionMs += delta;
            unsavedMs += delta;
        }
        lastTick = now;
    }

    // Adds the unsaved time to the stage total and to today's total of this account
    private void flush() {
        if (unsavedMs <= 0 || language == null) {
            return;
        }
        ProgressStore store = new ProgressStore(this, uid);
        store.addStudyMillis(unsavedMs);
        store.addStageStudyMillis(language, stageIndex, unsavedMs);
        unsavedMs = 0;
    }

    private void pauseInternal(boolean manual) {
        if (language == null || !running) {
            return;
        }
        addElapsed();
        flush();
        running = false;
        if (manual) {
            manualPaused = true;
        } else {
            autoPaused = true;
        }
        handler.removeCallbacks(tick); // no ticking while paused
        refresh();
    }

    private void resumeInternal(boolean manual) {
        if (language == null || running) {
            return;
        }
        if (manual) {
            manualPaused = false;
            autoPaused = false;
        } else if (manualPaused || !autoPaused) {
            return; // only an automatic pause is resumed automatically
        } else {
            autoPaused = false;
        }
        running = true;
        lastTick = SystemClock.elapsedRealtime();
        handler.removeCallbacks(tick);
        handler.post(tick);
    }

    private void endSession(boolean byUser) {
        handler.removeCallbacks(tick);
        if (language != null) {
            addElapsed();
            flush();
            if (byUser) {
                StudyTimerState.stoppedKey = language + "#" + stageIndex;
            }
        }
        language = null;
        running = false;
        StudyTimerState.publish(StudyTimerState.IDLE);
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    // ---- Screens and notification ----

    private void refresh() {
        StudyTimerState.publish(new StudyTimerState.Snapshot(true, running, manualPaused,
                language, stageTitle, stageIndex, sessionMs, unsavedMs));
        getSystemService(NotificationManager.class).notify(NOTIFICATION_ID, buildNotification());
    }

    private void createChannel() {
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                getString(R.string.timer_channel_name), NotificationManager.IMPORTANCE_LOW); // silent
        channel.setDescription(getString(R.string.timer_channel_description));
        getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }

    private Notification buildNotification() {
        String time = format(sessionMs);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(ContextCompat.getColor(this, R.color.primary))
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .setContentIntent(openStageIntent());

        if (running) {
            builder.setContentTitle(getString(R.string.timer_title_running, language, stageTitle, time));
            builder.addAction(0, getString(R.string.timer_action_pause), actionIntent(ACTION_TOGGLE, 1));
        } else {
            builder.setContentTitle(getString(R.string.timer_title_paused));
            builder.setContentText(getString(R.string.timer_text_paused, language, stageTitle, time));
            builder.addAction(0, getString(R.string.timer_action_resume), actionIntent(ACTION_TOGGLE, 1));
        }
        builder.addAction(0, getString(R.string.timer_action_stop), actionIntent(ACTION_STOP, 2));
        return builder.build();
    }

    private PendingIntent actionIntent(String action, int requestCode) {
        Intent intent = new Intent(this, StudyTimerService.class).setAction(action);
        return PendingIntent.getService(this, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    // Tapping the notification opens the stage being studied
    private PendingIntent openStageIntent() {
        Intent open = new Intent(this, CourseActivity.class);
        open.putExtra(LanguageDashboardActivity.EXTRA_LANGUAGE, language);
        open.putExtra(CourseActivity.EXTRA_STAGE_ID, stageId);
        open.putExtra(CourseActivity.EXTRA_STAGE_INDEX, stageIndex);
        open.putExtra(CourseActivity.EXTRA_STAGE_TITLE, stageTitle);
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(this, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}