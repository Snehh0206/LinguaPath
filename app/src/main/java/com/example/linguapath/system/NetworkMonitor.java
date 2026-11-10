package com.example.linguapath.system;

import android.content.Context;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.PowerManager;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

// Keeps the three facts about the phone that the app reacts to: online, airplane mode, battery saver.
// It listens only between start() and stop() (while the app is on screen).
public class NetworkMonitor {

    private static NetworkMonitor instance;

    public static synchronized NetworkMonitor get(Context context) {
        if (instance == null) {
            instance = new NetworkMonitor(context.getApplicationContext());
        }
        return instance;
    }

    // true when battery saver is on or the battery is low
    public static boolean isSavingPower(Context context) {
        return Boolean.TRUE.equals(get(context).savingPower.getValue());
    }

    private final Context context;
    private final ConnectivityManager connectivity;
    private final PowerManager power;
    private final MutableLiveData<Boolean> online = new MutableLiveData<>();
    private final MutableLiveData<Boolean> airplane = new MutableLiveData<>();
    private final MutableLiveData<Boolean> savingPower = new MutableLiveData<>();
    private final SystemEventsReceiver receiver = new SystemEventsReceiver(this);
    private boolean batteryLow = false;
    private boolean running = false;

    private NetworkMonitor(Context context) {
        this.context = context;
        connectivity = context.getSystemService(ConnectivityManager.class);
        power = context.getSystemService(PowerManager.class);
        refreshAll(); // correct values from the very start
    }

    public LiveData<Boolean> online() {
        return online;
    }

    public LiveData<Boolean> airplane() {
        return airplane;
    }

    public LiveData<Boolean> savingPower() {
        return savingPower;
    }

    public boolean isOnline() {
        return Boolean.TRUE.equals(online.getValue());
    }

    private void refreshAll() {
        online.setValue(isOnlineNow());
        airplane.setValue(Settings.Global.getInt(context.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) == 1);
        savingPower.setValue(batteryLow || power.isPowerSaveMode());
    }

    // ---- Called by the receiver ----

    void onAirplaneChanged(boolean on) {
        airplane.postValue(on);
    }

    void onPowerChanged() {
        savingPower.postValue(batteryLow || power.isPowerSaveMode());
    }

    void onBatteryLowChanged(boolean low) {
        batteryLow = low;
        onPowerChanged();
    }

    void recheckInternet() {
        online.postValue(isOnlineNow());
    }

    // ---- Internet ----

    private static boolean hasInternet(NetworkCapabilities caps) {
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }

    private boolean isOnlineNow() {
        Network network = connectivity.getActiveNetwork();
        if (network == null) {
            return false;
        }
        NetworkCapabilities caps = connectivity.getNetworkCapabilities(network);
        return caps != null && hasInternet(caps);
    }

    // The modern way to learn about connectivity changes (the old broadcast is deprecated)
    private final ConnectivityManager.NetworkCallback networkCallback =
            new ConnectivityManager.NetworkCallback() {
                @Override
                public void onCapabilitiesChanged(@NonNull Network network,
                                                  @NonNull NetworkCapabilities caps) {
                    online.postValue(hasInternet(caps));
                }

                @Override
                public void onLost(@NonNull Network network) {
                    online.postValue(false);
                }
            };

    // ---- Start and stop ----

    @SuppressWarnings("deprecation")
    public void start() {
        if (running) {
            return;
        }
        running = true;
        connectivity.registerDefaultNetworkCallback(networkCallback);

        IntentFilter filter = new IntentFilter();
        filter.addAction(android.content.Intent.ACTION_AIRPLANE_MODE_CHANGED);
        filter.addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED);
        filter.addAction(android.content.Intent.ACTION_BATTERY_LOW);
        filter.addAction(android.content.Intent.ACTION_BATTERY_OKAY);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        // All of these are protected system broadcasts: only Android itself can send them
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_EXPORTED);

        refreshAll(); // something may have changed while we were not listening
    }

    public void stop() {
        if (!running) {
            return;
        }
        running = false;
        try {
            connectivity.unregisterNetworkCallback(networkCallback);
            context.unregisterReceiver(receiver);
        } catch (IllegalArgumentException ignored) {
            // already unregistered
        }
    }
}