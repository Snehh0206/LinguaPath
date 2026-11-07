package com.example.linguapath.service;

import com.example.linguapath.data.ProgressStore;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

// Keeps the study-session time. There are no screens in here:
// Activities bind to it and just ask "how long?".
public class StudySessionService extends Service {

    private static final String TAG = "StudySessionService";
    private static final long AUTO_PAUSE_DELAY_MS = 5000;

    // The door that Activities use to reach this service
    public class LocalBinder extends Binder {
        public StudySessionService getService() {
            return StudySessionService.this;
        }
    }

    private final IBinder binder = new LocalBinder();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private long runningSince = 0;   // clock time when the timer last started, 0 = paused
    private long accumulatedMs = 0;  // time from earlier runs
    private String ownerUid = null;  // who this session belongs to

    // If no screen is bound for 5 seconds, the user left the app: pause
    private final Runnable autoPause = () -> {
        Log.d(TAG, "Nobody is looking, auto-pausing");
        pause();
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        return START_NOT_STICKY; // if the system kills it, don't bring it back
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        handler.removeCallbacks(autoPause);
        return binder;
    }

    @Override
    public void onRebind(Intent intent) {
        Log.d(TAG, "onRebind");
        handler.removeCallbacks(autoPause); // a screen came back in time (for example after rotation)
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "onUnbind");
        // Wait a little before pausing, so screen changes and rotation don't pause the timer
        handler.postDelayed(autoPause, AUTO_PAUSE_DELAY_MS);
        return true; // call onRebind when a screen binds again
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        handler.removeCallbacks(autoPause);
        pause();
        super.onDestroy();
    }

    // ---- What screens can ask for ----

    // A different user logged in? Start with a clean timer.
    public void attachUser(String uid) {
        if (ownerUid != null && !ownerUid.equals(uid)) {
            reset();
        }
        ownerUid = uid;
    }

    public boolean isRunning() {
        return runningSince != 0;
    }

    public long getElapsedMs() {
        long current = isRunning() ? SystemClock.elapsedRealtime() - runningSince : 0;
        return accumulatedMs + current;
    }

    public void start() {
        if (!isRunning()) {
            runningSince = SystemClock.elapsedRealtime();
        }
    }

    public void pause() {
        if (isRunning()) {
            long delta = SystemClock.elapsedRealtime() - runningSince;
            accumulatedMs += delta;
            runningSince = 0;
            // Save this stretch to the owner's daily total
            if (ownerUid != null) {
                new ProgressStore(this, ownerUid).addStudyMillis(delta);
            }
        }
    }

    public void toggle() {
        if (isRunning()) {
            pause();
        } else {
            start();
        }
    }

    public void reset() {
        pause();
        accumulatedMs = 0;
    }
}