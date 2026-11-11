package com.example.linguapath.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Reads the guided situations from assets/scenarios.json
public class ScenarioRepository {

    public static class Option {
        public final String text;
        public final String rom;     // empty for Spanish
        public final String meaning;

        Option(String text, String rom, String meaning) {
            this.text = text;
            this.rom = rom;
            this.meaning = meaning;
        }
    }

    public static class Step {
        public final String say;         // what the other person says
        public final String sayMeaning;
        public final String ask;         // what the learner has to do
        public final List<Option> options;
        public final int correct;

        Step(String say, String sayMeaning, String ask, List<Option> options, int correct) {
            this.say = say;
            this.sayMeaning = sayMeaning;
            this.ask = ask;
            this.options = options;
            this.correct = correct;
        }
    }

    public interface Callback {
        void onLoaded(List<Step> steps); // empty when this topic has no situation
    }

    private static final String TAG = "ScenarioRepository";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static void load(Context context, String topicId, String language, Callback callback) {
        Context app = context.getApplicationContext();
        executor.execute(() -> {
            List<Step> steps = parse(app, topicId, language);
            mainHandler.post(() -> callback.onLoaded(steps));
        });
    }

    private static List<Step> parse(Context context, String topicId, String language) {
        List<Step> steps = new ArrayList<>();
        try (InputStream in = context.getAssets().open("scenarios.json")) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int count;
            while ((count = in.read(buffer)) != -1) {
                out.write(buffer, 0, count);
            }
            JSONArray scenarios = new JSONObject(
                    new String(out.toByteArray(), StandardCharsets.UTF_8)).getJSONArray("scenarios");

            for (int i = 0; i < scenarios.length(); i++) {
                JSONObject scenario = scenarios.getJSONObject(i);
                if (!topicId.equals(scenario.getString("topicId"))) {
                    continue;
                }
                JSONArray list = scenario.optJSONArray(language);
                if (list == null) {
                    break;
                }
                for (int j = 0; j < list.length(); j++) {
                    JSONObject s = list.getJSONObject(j);
                    JSONArray o = s.getJSONArray("o");
                    List<Option> options = new ArrayList<>();
                    for (int k = 0; k < o.length(); k++) {
                        JSONObject opt = o.getJSONObject(k);
                        options.add(new Option(opt.getString("t"), opt.optString("r", ""), opt.getString("m")));
                    }
                    steps.add(new Step(s.getString("say"), s.getString("sm"), s.getString("ask"),
                            options, s.getInt("ok")));
                }
            }
        } catch (IOException | JSONException e) {
            Log.e(TAG, "Could not read scenarios.json", e);
        }
        return steps;
    }
}