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
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// The only class that talks to MyMemory.
// Order: saved translations first (free, offline), then the daily limit check, then the network.
// All work happens on a background thread. Answers come back on the main thread.
public class TranslationRepository {

    public interface Callback {
        void onSuccess(String translated, boolean fromCache);

        void onError(@StringRes int messageResId);
    }

    private static class QuotaException extends Exception {
    }

    private static final String TAG = "TranslationRepository";
    private static final String BASE_URL = "https://api.mymemory.translated.net/get";
    private static final int MAX_BYTES = 500; // MyMemory's limit for one request

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static void translate(Context context, String text, String targetLanguage,
                                 Callback callback) {
        Context appContext = context.getApplicationContext();

        executor.execute(() -> {
            String clean = text.trim();
            String pair = "en|" + languageCode(targetLanguage);
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

            // 3. Would this go over today's free limit? If so, block for the rest of the day.
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

            // The service can also report "limit reached" inside a normal answer
            if (status == 429 || json.optBoolean("quotaFinished", false)
                    || translated.startsWith("MYMEMORY WARNING")) {
                throw new QuotaException();
            }
            if (status != 200 || translated.isEmpty()) {
                throw new IOException("API status " + status);
            }

            // The service sometimes sends HTML codes like &#39;
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