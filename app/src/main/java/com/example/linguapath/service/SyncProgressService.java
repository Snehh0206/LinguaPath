package com.example.linguapath.service;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import com.example.linguapath.data.CloudSyncRepository;
import com.example.linguapath.data.ProgressStore;

// A BACKGROUND service: silent, does one job, then stops itself.
// Job: upload the progress of one language to Firebase.
public class SyncProgressService extends Service {

    public static final String EXTRA_LANGUAGE = "extra_language";
    private static final String TAG = "SyncProgressService";
    private static final long TIMEOUT_MS = 20_000;

    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String language = (intent == null) ? null : intent.getStringExtra(EXTRA_LANGUAGE);
        if (language == null) {
            stopSelf(startId);
            return START_NOT_STICKY;
        }
        Log.d(TAG, "Started: uploading " + language + " progress");

        ProgressStore store = new ProgressStore(this);
        store.setSyncPending(language, true); // until the server confirms

        // Never stay longer than 20 seconds. Firestore keeps the update and sends it later.
        Runnable giveUp = () -> {
            Log.d(TAG, "Timeout, stopping");
            stopSelf(startId);
        };
        handler.postDelayed(giveUp, TIMEOUT_MS);

        new CloudSyncRepository().uploadProgress(language, store.getCompletedCount(language), success -> {
            if (success) {
                store.setSyncPending(language, false);
            }
            Log.d(TAG, "Finished, success = " + success + ". Stopping.");
            handler.removeCallbacks(giveUp);
            stopSelf(startId); // the job is done: stop
        });
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null; // not a bound service
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}