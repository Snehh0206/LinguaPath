package com.example.linguapath.system;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.Observer;

import com.example.linguapath.R;

import java.util.HashMap;
import java.util.Map;

// Shows a small banner at the top of every screen: offline, checking, welcome back, battery saver.
// It starts the NetworkMonitor when the first screen appears and stops it when the last one is gone.
public class BannerController implements Application.ActivityLifecycleCallbacks {

    private static final long WELCOME_MS = 3500;
    private static final long RESULT_MS = 6000;
    private static final long CHECKING_MS = 8000;

    private final Context context;
    private final NetworkMonitor monitor;
    private final float density;
    private final Map<Activity, TextView> banners = new HashMap<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Observer<Boolean> stateObserver = value -> onStateChanged();
    private final Observer<String> resultObserver = this::onResult;

    private int startedCount = 0;
    private boolean wasOffline;
    private Boolean lastAirplane = null;
    private long welcomeUntil = 0;
    private long checkingUntil = 0;
    private long resultUntil = 0;
    private String resultText = "";

    public BannerController(Context context) {
        this.context = context.getApplicationContext();
        monitor = NetworkMonitor.get(context);
        density = context.getResources().getDisplayMetrics().density;
        wasOffline = !monitor.isOnline();
    }

    private int dp(int value) {
        return Math.round(value * density);
    }

    private void onStateChanged() {
        long now = SystemClock.uptimeMillis();
        boolean online = monitor.isOnline();
        boolean airplane = Boolean.TRUE.equals(monitor.airplane().getValue());

        // Airplane mode was just switched off: say that we are checking the connection
        if (lastAirplane != null && lastAirplane && !airplane) {
            checkingUntil = now + CHECKING_MS;
            handler.postDelayed(this::render, CHECKING_MS + 100);
        }
        lastAirplane = airplane;

        if (!online) {
            wasOffline = true;
        } else if (wasOffline) {
            // The connection is back: say welcome back, and retry what was waiting
            wasOffline = false;
            welcomeUntil = now + WELCOME_MS;
            handler.postDelayed(this::render, WELCOME_MS + 100);
            PendingWork.retryAll(context);
        }
        render();
    }

    private void onResult(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        resultText = text;
        resultUntil = SystemClock.uptimeMillis() + RESULT_MS;
        handler.postDelayed(this::render, RESULT_MS + 100);
        render();
    }

    // Chooses the message (the most important one wins)
    private void render() {
        long now = SystemClock.uptimeMillis();
        boolean online = monitor.isOnline();
        boolean airplane = Boolean.TRUE.equals(monitor.airplane().getValue());
        boolean saving = Boolean.TRUE.equals(monitor.savingPower().getValue());

        CharSequence text = null;
        int background = R.drawable.bg_card_butter;
        if (!online && airplane) {
            text = context.getString(R.string.banner_airplane);
        } else if (!online && now < checkingUntil) {
            text = context.getString(R.string.banner_checking);
        } else if (!online) {
            text = context.getString(R.string.banner_offline);
        } else if (now < resultUntil) {
            text = resultText;
            background = R.drawable.bg_card_sage;
        } else if (now < welcomeUntil) {
            text = context.getString(R.string.banner_back_online);
            background = R.drawable.bg_card_sage;
        } else if (saving) {
            text = context.getString(R.string.banner_power);
            background = R.drawable.bg_card_sky;
        }

        for (TextView banner : banners.values()) {
            if (text == null) {
                hide(banner);
            } else {
                show(banner, text, background);
            }
        }
    }

    private void show(TextView banner, CharSequence text, int background) {
        banner.setText(text);
        banner.setBackgroundResource(background);
        if (banner.getVisibility() != View.VISIBLE || banner.getAlpha() < 1f) {
            banner.animate().cancel();
            banner.setVisibility(View.VISIBLE);
            banner.setAlpha(0f);
            banner.setTranslationY(-dp(24));
            banner.animate().alpha(1f).translationY(0f).setDuration(250);
        }
    }

    private void hide(TextView banner) {
        if (banner.getVisibility() == View.VISIBLE) {
            banner.animate().cancel();
            banner.animate().alpha(0f).translationY(-dp(24)).setDuration(200)
                    .withEndAction(() -> banner.setVisibility(View.GONE));
        }
    }

    private void attach(Activity activity) {
        ViewGroup content = activity.findViewById(android.R.id.content);
        if (content == null || banners.containsKey(activity)) {
            return;
        }
        TextView banner = new TextView(activity);
        banner.setTextSize(14);
        banner.setTextColor(activity.getColor(R.color.ink));
        banner.setPadding(dp(16), dp(12), dp(16), dp(12));
        banner.setElevation(dp(12));
        banner.setVisibility(View.GONE);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP);
        params.setMargins(dp(16), dp(8), dp(16), 0);
        banner.setLayoutParams(params);

        // Keep the banner below the status bar
        ViewCompat.setOnApplyWindowInsetsListener(banner, (v, insets) -> {
            int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) v.getLayoutParams();
            lp.topMargin = dp(8) + top;
            v.setLayoutParams(lp);
            return insets;
        });

        content.addView(banner);
        ViewCompat.requestApplyInsets(banner);
        banners.put(activity, banner);
    }

    private void detach(Activity activity) {
        TextView banner = banners.remove(activity);
        if (banner != null && banner.getParent() instanceof ViewGroup) {
            ((ViewGroup) banner.getParent()).removeView(banner);
        }
    }

    @Override
    public void onActivityStarted(Activity activity) {
        if (startedCount++ == 0) {
            monitor.start();
            monitor.online().observeForever(stateObserver);
            monitor.airplane().observeForever(stateObserver);
            monitor.savingPower().observeForever(stateObserver);
            PendingWork.result().observeForever(resultObserver);
        }
        attach(activity);
        render();
    }

    @Override
    public void onActivityStopped(Activity activity) {
        detach(activity);
        if (--startedCount == 0) {
            monitor.online().removeObserver(stateObserver);
            monitor.airplane().removeObserver(stateObserver);
            monitor.savingPower().removeObserver(stateObserver);
            PendingWork.result().removeObserver(resultObserver);
            monitor.stop();
            handler.removeCallbacksAndMessages(null);
        }
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    }

    @Override
    public void onActivityResumed(Activity activity) {
    }

    @Override
    public void onActivityPaused(Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
    }
}