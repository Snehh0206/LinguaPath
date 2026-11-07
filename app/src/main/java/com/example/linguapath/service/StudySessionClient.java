package com.example.linguapath.service;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Locale;

// Used by Activities. It connects to the service and refreshes the time on screen once a second.
// bind() goes in onStart, unbind() goes in onStop.
public class StudySessionClient {

    public interface TimeListener {
        void onTime(long elapsedMs, boolean running);
    }

    private static final String TAG = "StudySessionClient";

    private final Context context;
    private final TimeListener listener;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private StudySessionService service;
    private boolean bound = false;

    // The one-second refresh. It is removed in unbind(), so it can never run twice.
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            publish();
            handler.postDelayed(this, 1000);
        }
    };

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            Log.d(TAG, "connected");
            service = ((StudySessionService.LocalBinder) binder).getService();
            service.attachUser(currentUserId());
            handler.removeCallbacks(tick);
            tick.run();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "disconnected");
            service = null;
            handler.removeCallbacks(tick);
        }
    };

    public StudySessionClient(Context context, TimeListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void bind() {
        if (bound) {
            return;
        }
        Intent intent = new Intent(context, StudySessionService.class);
        context.startService(intent); // started AND bound: the time survives screen changes
        bound = context.bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    public void unbind() {
        handler.removeCallbacks(tick);
        if (bound) {
            context.unbindService(connection);
            bound = false;
        }
        service = null;
    }

    public void toggle() {
        if (service != null) {
            service.toggle();
            publish();
        }
    }

    public void reset() {
        if (service != null) {
            service.reset();
            publish();
        }
    }

    private void publish() {
        if (service != null) {
            listener.onTime(service.getElapsedMs(), service.isRunning());
        }
    }

    private static String currentUserId() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return (user == null) ? "guest" : user.getUid();
    }

    public static String formatTime(long ms) {
        long totalSeconds = ms / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        if (hours > 0) {
            return String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format(Locale.US, "%02d:%02d", minutes, seconds);
    }
}