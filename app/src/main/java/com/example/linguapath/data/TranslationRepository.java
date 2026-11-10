package com.example.linguapath.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.util.Log;

import androidx.annotation.StringRes;

import com.example.linguapath.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

// The only class that talks to MyMemory.
// Order: saved translations first (free, offline), then the daily limit, then the network.
// If the phone is offline, the request is kept and retried when the connection returns.
public class TranslationRepository {

    public interface Callback {
        void onSuccess(String translated, boolean fromCache);

        void onError(@StringRes int messageResId);
    }

    public interface RetryListener {
        void onFinished(int translated);
    }

    private static class QuotaException extends Exception {
    }

    private static final String TAG = "TranslationRepository";
    private static final String BASE_URL = "https://api.mymemory.translated.net/get";
    private static final int MAX_BYTES = 500;

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    // English -> Spanish or Korean
    public static void translate(Context context, String text, String targetLanguage, Callback callback) {
        run(context, text, "en|" + languageCode(targetLanguage), callback);
    }

    // Spanish or Korean -> English (used by the sign photo translator)
    public static void translateToEnglish(Context context, String text, String sourceLanguage, Callback callback) {
        run(context, text, languageCode(sourceLanguage) + "|en", callback);
    }

    // Tries the waiting translations again. Every answer is saved in the cache.
    public static void retryPending(Context context, RetryListener listener) {
        Context app = context.getApplicationContext();
        List<String[]> items = PendingTranslations.takeAll(app);
        if (items.isEmpty()) {
            mainHandler.post(() -> listener.onFinished(0));
            return;
        }
        AtomicInteger left = new AtomicInteger(items.size());
        AtomicInteger done = new AtomicInteger(0);
        for (String[] item : items) {
            run(app, item[1], item[0], new Callback() {
                @Override
                public void onSuccess(String translated, boolean fromCache) {
                    done.incrementAndGet();
                    if (left.decrementAndGet() == 0) {
                        listener.onFinished(done.get());
                    }
                }

                @Override
                public void onError(int messageResId) {
                    // Offline again: run() has put it back in the waiting list
                    if (left.decrementAndGet() == 0) {
                        listener.onFinished(done.get());
                    }
                }
            });
        }
    }

    private static void run(Context context, String text, String pair, Callback callback) {
        Context appContext = context.getApplicationContext();

        executor.execute(() -> {
            String clean = text.trim();
            String key = clean.toLowerCase(Locale.ROOT);
            TranslationDao dao = AppDatabase.getInstance(appContext).translationDao();

            // 1. Already translated before? Instant, free and works offline.
            CachedTranslation cached = dao.find(pair, key);
            if (cached != null) {
                postSuccess(callback, cached.translatedText, true);
                return;
            }

            // 2. Too long for the API?
            if (clean.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
                postError(callback, R.string.translator_too_long);
                return;
            }

            // 3. Would this go over today's safe limit? Then stop for the rest of the day.
            TranslationUsage usage = new TranslationUsage(appContext);
            if (!usage.canUse(clean.length())) {
                usage.blockForToday();
                postError(callback, R.string.translator_quota);
                return;
            }

            // 4. Ask the API
            try {
                String translated = fetch(clean, pair);
                dao.insert(new CachedTranslation(pair, key, translated, System.currentTimeMillis()));
                usage.add(clean.length());
                postSuccess(callback, translated, false);
            } catch (QuotaException e) {
                usage.blockForToday();
                postError(callback, R.string.translator_quota);
            } catch (IOException e) {
                Log.w(TAG, "Network problem: " + e.getMessage());
                PendingTranslations.add(appContext, pair, clean); // retried when we are back online
                postError(callback, R.string.translator_offline);
            } catch (JSONException e) {
                Log.e(TAG, "Could not read the answer", e);
                postError(callback, R.string.translator_error);
            }
        });
    }

    private static String languageCode(String language) {
        return "Korean".equals(language) ? "ko" : "es";
    }

    // One GET request, then read the JSON answer:
    // { "responseData": { "translatedText": "..." }, "responseStatus": 200 }
    private static String fetch(String text, String pair)
            throws IOException, JSONException, QuotaException {
        String url = BASE_URL
                + "?q=" + URLEncoder.encode(text, "UTF-8")
                + "&langpair=" + URLEncoder.encode(pair, "UTF-8");

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        try {
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(8000);
            connection.setRequestMethod("GET");

            int httpCode = connection.getResponseCode();
            if (httpCode == 429) {
                throw new QuotaException();
            }
            if (httpCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP " + httpCode);
            }

            JSONObject json = new JSONObject(readAll(connection.getInputStream()));
            int status = json.optInt("responseStatus", 0);
            String translated = json.getJSONObject("responseData").optString("translatedText", "");

            if (status == 429 || json.optBoolean("quotaFinished", false)
                    || translated.startsWith("MYMEMORY WARNING")) {
                throw new QuotaException();
            }
            if (status != 200 || translated.isEmpty()) {
                throw new IOException("API status " + status);
            }
            return Html.fromHtml(translated, Html.FROM_HTML_MODE_LEGACY).toString();
        } finally {
            connection.disconnect();
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

    private static void postSuccess(Callback callback, String translated, boolean fromCache) {
        mainHandler.post(() -> callback.onSuccess(translated, fromCache));
    }

    private static void postError(Callback callback, @StringRes int messageResId) {
        mainHandler.post(() -> callback.onError(messageResId));
    }
}