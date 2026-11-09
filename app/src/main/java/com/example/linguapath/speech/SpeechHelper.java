package com.example.linguapath.speech;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;

import com.example.linguapath.R;

import java.util.Locale;

// Speaks Korean and Spanish with Android's built-in engine.
// Create it in onStart, call shutdown() in onStop, so the engine never keeps running.
public class SpeechHelper implements TextToSpeech.OnInitListener {

    private static final String TAG = "SpeechHelper";

    private final Activity activity;
    private TextToSpeech tts;
    private boolean ready = false;
    private boolean failed = false;

    public SpeechHelper(Activity activity) {
        this.activity = activity;
        // The engine starts in the background. onInit() tells us when it is ready.
        tts = new TextToSpeech(activity.getApplicationContext(), this);
    }

    @Override
    public void onInit(int status) {
        ready = (status == TextToSpeech.SUCCESS);
        failed = !ready;
        if (ready) {
            tts.setSpeechRate(0.9f); // a little slower, easier for learners
        }
        Log.d(TAG, "engine ready = " + ready);
    }

    public void speak(String text, String language) {
        if (text == null || text.trim().isEmpty()) {
            return;
        }
        if (failed) {
            showUnavailableDialog();
            return;
        }
        if (!ready || tts == null) {
            Log.d(TAG, "engine still starting, try again in a moment");
            return;
        }

        int result = tts.setLanguage(localeFor(language));
        if (result < TextToSpeech.LANG_AVAILABLE) {
            // LANG_MISSING_DATA or LANG_NOT_SUPPORTED: the voice is not on this phone
            showMissingVoiceDialog(language);
            return;
        }
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "linguapath");
    }

    public void shutdown() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
        ready = false;
    }

    private Locale localeFor(String language) {
        return "Korean".equals(language)
                ? Locale.forLanguageTag("ko-KR")
                : Locale.forLanguageTag("es-ES");
    }

    // ---- Friendly problems ----

    private void showMissingVoiceDialog(String language) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        new AlertDialog.Builder(activity)
                .setTitle(R.string.speech_missing_title)
                .setMessage(activity.getString(R.string.speech_missing_message, language))
                .setPositiveButton(R.string.speech_install, (dialog, which) -> openVoiceInstall())
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showUnavailableDialog() {
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        new AlertDialog.Builder(activity)
                .setTitle(R.string.speech_unavailable_title)
                .setMessage(R.string.speech_unavailable_message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void openVoiceInstall() {
        try {
            activity.startActivity(new Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA));
        } catch (ActivityNotFoundException e) {
            // No installer screen on this phone: open the general Settings instead
            try {
                activity.startActivity(new Intent(Settings.ACTION_SETTINGS));
            } catch (ActivityNotFoundException ignored) {
                Log.w(TAG, "No settings screen found");
            }
        }
    }
}