package com.example.linguapath.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import com.example.linguapath.speech.SpeechHelper;

import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;
import androidx.core.content.ContextCompat;

import com.example.linguapath.CourseActivity;
import com.example.linguapath.LanguageDashboardActivity;
import com.example.linguapath.R;
import com.example.linguapath.data.AppDatabase;
import com.example.linguapath.data.WordEntity;
import com.example.linguapath.model.Word;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// A FOREGROUND service: it plays the words of a stage with text-to-speech, even with the screen locked.
// Two modes: "play all pronunciations" and "audio lesson" (word, meaning, example, translation).
public class PlaybackService extends Service {

    public static final String ACTION_START = "com.example.linguapath.PLAY_START";
    public static final String ACTION_TOGGLE = "com.example.linguapath.PLAY_TOGGLE";
    public static final String ACTION_NEXT = "com.example.linguapath.PLAY_NEXT";
    public static final String ACTION_STOP = "com.example.linguapath.PLAY_STOP";

    public static final String EXTRA_LESSON = "extra_lesson";
    public static final String EXTRA_LANGUAGE = "extra_language";
    public static final String EXTRA_STAGE_ID = "extra_stage_id";
    public static final String EXTRA_STAGE_INDEX = "extra_stage_index";
    public static final String EXTRA_STAGE_TITLE = "extra_stage_title";

    private static final String TAG = "PlaybackService";
    private static final String CHANNEL_ID = "playback";
    private static final int NOTIFICATION_ID = 3001;
    private static final long GAP_MS = 700;
    private static final long WAKE_LOCK_MS = 30 * 60 * 1000L;

    private static PlaybackService instance;

    // One thing to say: some text in some language
    private static class Step {
        final String text;
        final Locale locale;
        final boolean target; // true = the language being learned

        Step(String text, Locale locale, boolean target) {
            this.text = text;
            this.locale = locale;
            this.target = target;
        }
    }

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<Word> words = new ArrayList<>();
    private TextToSpeech tts;
    private boolean ttsReady, wordsLoaded, begun;
    private boolean lesson, playing, userPaused;
    private String language = "";
    private String stageTitle = "";
    private int stageId, stageIndex, wordIndex, stepIndex;
    private int token;    // changes whenever speech is stopped, so old "finished" signals are ignored
    private int session;  // changes whenever a new playback starts
    private int errorRes;
    private String errorArg = "";
    private PowerManager.WakeLock wakeLock;
    private AudioManager audio;
    private AudioFocusRequest focusRequest;

    private final Runnable speakStep = this::speakCurrent;

    // ---- Calls from screens (same process, no binding) ----

    static void toggleNow() {
        if (instance != null) {
            if (instance.playing) {
                instance.pause(true);
            } else {
                instance.resume(true);
            }
        }
    }

    static void nextNow() {
        if (instance != null) {
            instance.next();
        }
    }

    static void stopNow() {
        if (instance != null) {
            instance.stopPlayback();
        }
    }

    // ---- Lifecycle ----

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        audio = getSystemService(AudioManager.class);
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
            startPlayback(intent);
        } else if (ACTION_TOGGLE.equals(action)) {
            toggleNow();
        } else if (ACTION_NEXT.equals(action)) {
            nextNow();
        } else if (ACTION_STOP.equals(action)) {
            stopPlayback();
        } else if (!begun) {
            stopSelf();
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        handler.removeCallbacksAndMessages(null);
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
        releaseWakeLock();
        abandonFocus();
        instance = null;
        super.onDestroy();
    }

    // ---- Starting ----

    private void startPlayback(Intent intent) {
        token++;
        session++;
        handler.removeCallbacksAndMessages(null);
        if (tts != null) {
            tts.stop();
        }

        lesson = intent.getBooleanExtra(EXTRA_LESSON, false);
        language = nonNull(intent.getStringExtra(EXTRA_LANGUAGE));
        stageTitle = nonNull(intent.getStringExtra(EXTRA_STAGE_TITLE));
        stageId = intent.getIntExtra(EXTRA_STAGE_ID, 0);
        stageIndex = intent.getIntExtra(EXTRA_STAGE_INDEX, 0);
        wordIndex = 0;
        stepIndex = 0;
        playing = true;
        userPaused = false;
        begun = false;
        wordsLoaded = false;
        errorRes = 0;
        errorArg = "";
        words.clear();

        // A foreground service must show its notification right away
        createChannel();
        ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        acquireWakeLock(); // keeps the CPU awake while the screen is off
        publish();

        // Load the stage's words in the background
        final int mySession = session;
        AppDatabase.databaseExecutor.execute(() -> {
            List<WordEntity> list = AppDatabase.getInstance(getApplicationContext())
                    .contentDao().getWordsForStage(stageId);
            handler.post(() -> {
                if (mySession != session) {
                    return;
                }
                for (WordEntity e : list) {
                    words.add(new Word(e.id, e.word, e.romanization, e.meaning, e.example, e.exampleMeaning));
                }
                wordsLoaded = true;
                maybeBegin();
            });
        });
        ensureTts();
    }

    private void ensureTts() {
        if (tts != null) {
            maybeBegin();
            return;
        }
        tts = new TextToSpeech(getApplicationContext(), status -> handler.post(() -> {
            if (tts == null) {
                return; // stopped while it was starting
            }
            if (status != TextToSpeech.SUCCESS) {
                fail(R.string.speech_unavailable_message, "");
                return;
            }
            ttsReady = true;
            tts.setSpeechRate(0.9f);
            tts.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build());
            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String utteranceId) {
                }

                @Override
                public void onDone(String utteranceId) {
                    handler.post(() -> onUtteranceFinished(utteranceId));
                }

                @Override
                public void onError(String utteranceId) {
                    handler.post(() -> onUtteranceFinished(utteranceId));
                }
            });
            maybeBegin();
        }));
    }

    private void maybeBegin() {
        if (begun || !wordsLoaded || !ttsReady || !playing) {
            return;
        }
        begun = true;
        if (words.isEmpty()) {
            fail(R.string.playback_no_words, "");
            return;
        }
        requestFocus();
        speakCurrent();
    }

    // ---- Speaking ----

    private List<Step> stepsFor(Word w) {
        Locale target = SpeechHelper.localeFor(language);
        Locale english = Locale.forLanguageTag("en-US");

        List<Step> steps = new ArrayList<>();
        steps.add(new Step(w.getWord(), target, true));
        if (lesson) {
            steps.add(new Step(w.getMeaning(), english, false));
            if (!w.getExample().isEmpty()) {
                steps.add(new Step(w.getExample(), target, true));
                steps.add(new Step(w.getExampleMeaning(), english, false));
            }
        }
        return steps;
    }

    private void speakCurrent() {
        if (!playing || !ttsReady || tts == null || words.isEmpty()) {
            return;
        }
        if (wordIndex >= words.size()) {
            stopPlayback(); // the last word is done
            return;
        }
        List<Step> steps = stepsFor(words.get(wordIndex));
        if (stepIndex >= steps.size()) {
            wordIndex++;
            stepIndex = 0;
            speakCurrent();
            return;
        }

        Step step = steps.get(stepIndex);
        int result = tts.setLanguage(step.locale);
        if (result < TextToSpeech.LANG_AVAILABLE) {
            if (step.target) {
                fail(R.string.speech_missing_message, language); // the voice is not installed
                return;
            }
            stepIndex++; // no English voice: skip the English part
            speakCurrent();
            return;
        }

        token++;
        refresh();
        tts.speak(step.text, TextToSpeech.QUEUE_FLUSH, null, "u" + token);
    }

    private void onUtteranceFinished(String utteranceId) {
        if (!("u" + token).equals(utteranceId) || !playing) {
            return; // an old signal
        }
        stepIndex++;
        handler.postDelayed(speakStep, GAP_MS);
    }

    // ---- Controls ----

    private void pause(boolean byUser) {
        if (!playing) {
            return;
        }
        playing = false;
        if (byUser) {
            userPaused = true;
        }
        token++;
        handler.removeCallbacks(speakStep);
        if (tts != null) {
            tts.stop();
        }
        refresh();
    }

    private void resume(boolean byUser) {
        if (playing || !begun) {
            return;
        }
        if (!byUser && userPaused) {
            return; // only automatic pauses are resumed automatically
        }
        userPaused = false;
        playing = true;
        requestFocus();
        refresh();
        speakCurrent();
    }

    private void next() {
        if (!begun) {
            return;
        }
        token++;
        handler.removeCallbacks(speakStep);
        if (tts != null) {
            tts.stop();
        }
        wordIndex++;
        stepIndex = 0;
        playing = true;
        userPaused = false;
        if (wordIndex >= words.size()) {
            stopPlayback();
        } else {
            speakCurrent();
        }
    }

    private void fail(int messageRes, String arg) {
        errorRes = messageRes;
        errorArg = arg;
        stopPlayback();
    }

    private void stopPlayback() {
        token++;
        session++;
        handler.removeCallbacksAndMessages(null);
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
        ttsReady = false;
        begun = false;
        playing = false;
        releaseWakeLock();
        abandonFocus();
        PlaybackState.publish(new PlaybackState.Snapshot(false, false, lesson, language, stageTitle,
                0, 0, "", errorRes, errorArg));
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    // ---- Audio focus (so we pause when a call or other audio takes over) ----

    private void requestFocus() {
        focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build())
                .setOnAudioFocusChangeListener(change -> handler.post(() -> {
                    if (change == AudioManager.AUDIOFOCUS_LOSS
                            || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                        pause(false);
                    } else if (change == AudioManager.AUDIOFOCUS_GAIN) {
                        resume(false);
                    }
                }))
                .build();
        audio.requestAudioFocus(focusRequest);
    }

    private void abandonFocus() {
        if (audio != null && focusRequest != null) {
            audio.abandonAudioFocusRequest(focusRequest);
            focusRequest = null;
        }
    }

    private void acquireWakeLock() {
        if (wakeLock == null) {
            wakeLock = getSystemService(PowerManager.class)
                    .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "linguapath:playback");
        }
        if (!wakeLock.isHeld()) {
            wakeLock.acquire(WAKE_LOCK_MS); // at most 30 minutes
        }
    }

    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    // ---- Screens and notification ----

    private String currentWord() {
        return wordIndex < words.size() ? words.get(wordIndex).getWord() : "";
    }

    private void publish() {
        PlaybackState.publish(new PlaybackState.Snapshot(true, playing, lesson, language, stageTitle,
                wordIndex, words.size(), currentWord(), 0, ""));
    }

    private void refresh() {
        publish();
        getSystemService(NotificationManager.class).notify(NOTIFICATION_ID, buildNotification());
    }

    private void createChannel() {
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                getString(R.string.playback_channel_name), NotificationManager.IMPORTANCE_LOW);
        channel.setDescription(getString(R.string.playback_channel_description));
        getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }

    private Notification buildNotification() {
        String title = getString(lesson ? R.string.playback_title_lesson : R.string.playback_title_words, stageTitle);
        String text = words.isEmpty()
                ? getString(R.string.playback_getting_ready)
                : getString(R.string.playback_progress,
                Math.min(wordIndex + 1, words.size()), words.size(), currentWord());

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(ContextCompat.getColor(this, R.color.primary))
                .setContentTitle(title)
                .setContentText(text)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // visible on the lock screen
                .setContentIntent(openStageIntent());

        builder.addAction(0, getString(playing ? R.string.playback_action_pause : R.string.playback_action_play),
                actionIntent(ACTION_TOGGLE, 11));
        builder.addAction(0, getString(R.string.playback_action_next), actionIntent(ACTION_NEXT, 12));
        builder.addAction(0, getString(R.string.playback_action_stop), actionIntent(ACTION_STOP, 13));
        return builder.build();
    }

    private PendingIntent actionIntent(String action, int requestCode) {
        Intent intent = new Intent(this, PlaybackService.class).setAction(action);
        return PendingIntent.getService(this, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

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

    private static String nonNull(String value) {
        return value == null ? "" : value;
    }
}