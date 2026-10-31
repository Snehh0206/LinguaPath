package com.example.linguapath.data;

import android.content.Context;
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

// Copies content.json into the database the first time the app runs.
// Always call this on a background thread.
public class DataImporter {

    private static final String TAG = "DataImporter";

    public static void importIfNeeded(Context context, AppDatabase db) {
        ContentDao dao = db.contentDao();

        // Already filled? Then there is nothing to do.
        if (dao.countStages() > 0) {
            return;
        }

        try {
            JSONObject root = new JSONObject(readAsset(context, "content.json"));
            JSONArray languages = root.getJSONArray("languages");

            for (int i = 0; i < languages.length(); i++) {
                JSONObject language = languages.getJSONObject(i);
                String languageName = language.getString("name");
                JSONArray stages = language.getJSONArray("stages");

                for (int s = 0; s < stages.length(); s++) {
                    JSONObject stageJson = stages.getJSONObject(s);

                    StageEntity stage = new StageEntity(
                            languageName,
                            stageJson.getString("title"),
                            stageJson.getString("level"),
                            s);
                    int stageId = (int) dao.insertStage(stage);

                    JSONArray words = stageJson.getJSONArray("words");
                    List<WordEntity> wordList = new ArrayList<>();
                    for (int w = 0; w < words.length(); w++) {
                        JSONObject word = words.getJSONObject(w);
                        wordList.add(new WordEntity(
                                stageId,
                                word.getString("word"),
                                word.getString("romanization"),
                                word.getString("meaning"),
                                word.getString("example"),
                                word.getString("exampleMeaning")));
                    }
                    dao.insertWords(wordList);
                }
            }
            Log.d(TAG, "Content imported");
        } catch (IOException | JSONException e) {
            Log.e(TAG, "Could not import content.json", e);
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