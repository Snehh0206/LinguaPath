package com.example.linguapath.system;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.PowerManager;

// Receives the system broadcasts we care about:
//   airplane mode on/off, battery saver on/off, battery low/ok, and connectivity changes.
// It is registered only while the app is on screen. onReceive() stays short.
public class SystemEventsReceiver extends BroadcastReceiver {

    private final NetworkMonitor monitor;

    SystemEventsReceiver(NetworkMonitor monitor) {
        this.monitor = monitor;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) {
            return;
        }
        switch (action) {
            case Intent.ACTION_AIRPLANE_MODE_CHANGED:
                monitor.onAirplaneChanged(intent.getBooleanExtra("state", false));
                break;
            case PowerManager.ACTION_POWER_SAVE_MODE_CHANGED:
                monitor.onPowerChanged();
                break;
            case Intent.ACTION_BATTERY_LOW:
                monitor.onBatteryLowChanged(true);
                break;
            case Intent.ACTION_BATTERY_OKAY:
                monitor.onBatteryLowChanged(false);
                break;
            case ConnectivityManager.CONNECTIVITY_ACTION:
                monitor.recheckInternet();
                break;
            default:
                break;
        }
    }
}