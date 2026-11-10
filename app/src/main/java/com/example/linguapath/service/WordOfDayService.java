package com.example.linguapath.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import com.example.linguapath.data.AppDatabase;
import com.example.linguapath.data.TranslationRepository;
import com.example.linguapath.data.WordOfDay;
import com.example.linguapath.data.WordOfDayDao;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

// A BACKGROUND service: silent, does one job, then stops itself.
// Job: ask the free Datamuse API for an English word, translate it with MyMemory,
// save it in Room. The dashboard shows it the next time it opens.
public class WordOfDayService extends Service {

    private static final String TAG = "WordOfDayService";
    private static final String PREFS = "linguapath_wod";
    private static final long MIN_GAP_MS = 30 * 60 * 1000L;
    private static final String[] LANGUAGES = {"Spanish", "Korean"};
    // Today's theme decides which words Datamuse suggests
    private static final String[] THEMES = {"travel", "food", "family", "home", "weather", "school",
            "work", "city", "nature", "health", "shopping", "music"};

    private static final ExecutorService worker = Executors.newSingleThreadExecutor();

    // The dashboard calls this. It starts the service only when a new word is really needed.
    public static void requestIfNeeded(Context context) {
        Context app = context.getApplicationContext();
        SharedPreferences prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        if (LocalDate.now().toString().equals(prefs.getString("done_day", ""))) {
            return; // today's word is already saved
        }
        if (System.currentTimeMillis() - prefs.getLong("last_try", 0) < MIN_GAP_MS) {
            return; // not more often than every 30 minutes
        }
        if (app.getSystemService(PowerManager.class).isPowerSaveMode()) {
            Log.d(TAG, "Battery saver is on, skipping the word of the day");
            return; // optional work is skipped to save power
        }
        if (!isOnline(app)) {
            return; // retried when the connection returns
        }
        prefs.edit().putLong("last_try", System.currentTimeMillis()).apply();
        app.startService(new Intent(app, WordOfDayService.class));
    }

    private static boolean isOnline(Context context) {
        ConnectivityManager cm = context.getSystemService(ConnectivityManager.class);
        Network network = cm.getActiveNetwork();
        if (network == null) {
            return false;
        }
        NetworkCapabilities caps = cm.getNetworkCapabilities(network);
        return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Started");
        worker.execute(() -> {
            try {
                doJob();
            } finally {
                Log.d(TAG, "Finished. Stopping.");
                stopSelf(startId); // the job is done: stop
            }
        });
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null; // not a bound service
    }

    private void doJob() {
        Context app = getApplicationContext();
        String today = LocalDate.now().toString();
        WordOfDayDao dao = AppDatabase.getInstance(app).wordOfDayDao();

        List<String> needed = new ArrayList<>();
        for (String language : LANGUAGES) {
            if (dao.find(today, language) == null) {
                needed.add(language);
            }
        }
        if (needed.isEmpty()) {
            markDone(app, today);
            return;
        }

        String english = fetchEnglishWord();
        if (english == null) {
            return;
        }

        // Translate it for each language. We are on a background thread, so waiting here is fine.
        Map<String, String> results = new ConcurrentHashMap<>();
        CountDownLatch latch = new CountDownLatch(needed.size());
        for (String language : needed) {
            TranslationRepository.translate(app, english, language, new TranslationRepository.Callback() {
                @Override
                public void onSuccess(String translated, boolean fromCache) {
                    results.put(language, translated);
                    latch.countDown();
                }

                @Override
                public void onError(int messageResId) {
                    latch.countDown();
                }
            });
        }
        try {
            latch.await(20, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        for (Map.Entry<String, String> entry : results.entrySet()) {
            dao.insert(new WordOfDay(today, entry.getKey(), english, entry.getValue(),
                    System.currentTimeMillis()));
            Log.d(TAG, "Saved " + english + " -> " + entry.getValue() + " (" + entry.getKey() + ")");
        }
        if (!results.isEmpty()) {
            markDone(app, today);
        }
    }

    private void markDone(Context app, String today) {
        app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("done_day", today).apply();
    }

    // Asks Datamuse for words related to today's theme and picks a common noun
    private String fetchEnglishWord() {
        int day = LocalDate.now().getDayOfYear();
        String theme = THEMES[day % THEMES.length];
        HttpURLConnection connection = null;
        try {
            URL url = new URL("https://api.datamuse.com/words?ml=" + URLEncoder.encode(theme, "UTF-8")
                    + "&md=pf&max=60");
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(8000);
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return null;
            }

            JSONArray array = new JSONArray(readAll(connection.getInputStream()));
            List<String> good = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.getJSONObject(i);
                String word = item.optString("word", "");
                if (!word.matches("[a-z]{4,9}")) {
                    continue; // only simple single words
                }
                boolean noun = false;
                double frequency = 0;
                JSONArray tags = item.optJSONArray("tags");
                if (tags != null) {
                    for (int j = 0; j < tags.length(); j++) {
                        String tag = tags.getString(j);
                        if ("n".equals(tag)) {
                            noun = true;
                        } else if (tag.startsWith("f:")) {
                            frequency = Double.parseDouble(tag.substring(2));
                        }
                    }
                }
                if (noun && frequency >= 2.0) {
                    good.add(word); // a noun that people really use
                }
            }
            if (good.isEmpty()) {
                return null;
            }
            return good.get(day % good.size());
        } catch (IOException | JSONException | NumberFormatException e) {
            Log.w(TAG, "Could not get a word: " + e.getMessage());
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static String readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int count;
        while ((count = in.read(buffer)) != -1) {
            out.write(buffer, 0, count);
        }
        in.close();
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }
}