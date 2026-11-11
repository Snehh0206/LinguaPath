package com.example.linguapath.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.linguapath.model.Topic;
import com.example.linguapath.model.TopicWord;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Reads topics.json once and keeps it in memory
public class TopicRepository {

    public interface Callback {
        void onLoaded(List<Topic> topics);
    }

    private static final String TAG = "TopicRepository";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static List<Topic> cache;

    // For screens: the work runs in the background, the answer comes back on the main thread
    public static void load(Context context, Callback callback) {
        Context appContext = context.getApplicationContext();
        executor.execute(() -> {
            List<Topic> topics = loadBlocking(appContext);
            mainHandler.post(() -> callback.onLoaded(topics));
        });
    }

    // For code that is already on a background thread
    public static synchronized List<Topic> loadBlocking(Context context) {
        if (cache != null) {
            return cache;
        }
        List<Topic> topics = new ArrayList<>();
        try {
            JSONObject root = new JSONObject(readAsset(context, "topics.json"));
            JSONArray array = root.getJSONArray("topics");
            for (int i = 0; i < array.length(); i++) {
                JSONObject t = array.getJSONObject(i);
                Topic topic = new Topic(t.getString("id"), t.getString("title"),
                        t.getString("emoji"), t.getInt("theme"));

                JSONObject words = t.getJSONObject("words");
                Iterator<String> languages = words.keys();
                while (languages.hasNext()) {
                    String language = languages.next();
                    JSONArray list = words.getJSONArray(language);
                    List<TopicWord> parsed = new ArrayList<>();
                    for (int j = 0; j < list.length(); j++) {
                        JSONObject w = list.getJSONObject(j);
                        parsed.add(new TopicWord(w.getString("word"), w.getString("romanization"),
                                w.getString("meaning"), w.getString("emoji")));
                    }
                    topic.setWords(language, parsed);
                }
                topics.add(topic);
            }
            addBangla(context, topics);
        } catch (IOException | JSONException e) {
            Log.e(TAG, "Could not read topics.json", e);
        }
        if (!topics.isEmpty()) {
            cache = topics;
        }
        return topics;
    }
    // Adds the Bangla words from topics_bangla.json to the topics we already loaded
    private static void addBangla(Context context, List<Topic> topics) {
        try {
            JSONObject bn = new JSONObject(readAsset(context, "topics_bangla.json"));
            for (Topic topic : topics) {
                JSONArray list = bn.optJSONArray(topic.id);
                if (list == null) {
                    continue;
                }
                List<TopicWord> parsed = new ArrayList<>();
                for (int j = 0; j < list.length(); j++) {
                    JSONObject w = list.getJSONObject(j);
                    parsed.add(new TopicWord(w.getString("word"), w.getString("romanization"),
                            w.getString("meaning"), w.getString("emoji")));
                }
                topic.setWords("Bangla", parsed);
            }
        } catch (IOException | JSONException e) {
            Log.e(TAG, "Could not read topics_bangla.json", e);
        }
    }
    private static String readAsset(Context context, String fileName) throws IOException {
        InputStream in = context.getAssets().open(fileName);
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