package com.example.linguapath.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Loads the lessons into the database.
// 1. content.json (the starter stages) the first time.
// 2. Every file in assets/packs/ that was not imported yet, in name order.
// A pack adds new stages after the existing ones, so nothing that already exists moves.
// Always call this on a background thread.
public class DataImporter {

    private static final String TAG = "DataImporter";
    private static final String PACK_DIR = "packs";
    private static final String PREFS = "linguapath_content";
    private static final String KEY_PACKS = "imported_packs";

    // One stage waiting to be saved
    private static class PendingStage {
        final String language;
        final String title;
        final String level;
        final List<String[]> words = new ArrayList<>(); // word, romanization, meaning, example, exampleMeaning

        PendingStage(String language, String title, String level) {
            this.language = language;
            this.title = title;
            this.level = level;
        }
    }

    public static void importIfNeeded(Context context, AppDatabase db) {
        ContentDao dao = db.contentDao();
        SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        if (dao.countStages() == 0) {
            // A fresh database: forget old pack records and load the starter stages first
            prefs.edit().remove(KEY_PACKS).apply();
            importStarter(context, dao);
        }
        importNewPacks(context, db, prefs);
    }

    // ---- The starter stages (content.json) ----

    private static void importStarter(Context context, ContentDao dao) {
        try {
            JSONObject root = new JSONObject(readAsset(context, "content.json"));
            JSONArray languages = root.getJSONArray("languages");

            for (int i = 0; i < languages.length(); i++) {
                JSONObject language = languages.getJSONObject(i);
                String languageName = language.getString("name");
                JSONArray stages = language.getJSONArray("stages");

                for (int s = 0; s < stages.length(); s++) {
                    JSONObject stageJson = stages.getJSONObject(s);
                    StageEntity stage = new StageEntity(languageName,
                            stageJson.getString("title"), stageJson.getString("level"), s);
                    int stageId = (int) dao.insertStage(stage);

                    JSONArray words = stageJson.getJSONArray("words");
                    List<WordEntity> wordList = new ArrayList<>();
                    for (int w = 0; w < words.length(); w++) {
                        JSONObject word = words.getJSONObject(w);
                        wordList.add(new WordEntity(stageId,
                                word.getString("word"), word.getString("romanization"),
                                word.getString("meaning"), word.getString("example"),
                                word.getString("exampleMeaning")));
                    }
                    dao.insertWords(wordList);
                }
            }
            Log.d(TAG, "Starter content imported");
        } catch (IOException | JSONException e) {
            Log.e(TAG, "Could not import content.json", e);
        }
    }

    // ---- The packs ----

    private static void importNewPacks(Context context, AppDatabase db, SharedPreferences prefs) {
        String[] files;
        try {
            files = context.getAssets().list(PACK_DIR);
        } catch (IOException e) {
            return;
        }
        if (files == null) {
            return;
        }
        Arrays.sort(files); // pack_01, pack_02, ... Never rename or remove a pack that was imported.

        Set<String> done = new HashSet<>(prefs.getStringSet(KEY_PACKS, new HashSet<>()));
        for (String file : files) {
            if (!file.endsWith(".json") || done.contains(file)) {
                continue;
            }
            if (!importPack(context, db, file)) {
                break; // stop at the first problem, so the order stays intact
            }
            done.add(file);
            prefs.edit().putStringSet(KEY_PACKS, new HashSet<>(done)).apply();
        }
    }

    private static boolean importPack(Context context, AppDatabase db, String file) {
        try {
            JSONObject root = new JSONObject(readAsset(context, PACK_DIR + "/" + file));
            JSONArray languages = root.getJSONArray("languages");

            // Read the whole file first. A typo imports nothing instead of half a pack.
            List<PendingStage> pending = new ArrayList<>();
            for (int i = 0; i < languages.length(); i++) {
                JSONObject language = languages.getJSONObject(i);
                String languageName = language.getString("name");
                JSONArray stages = language.getJSONArray("stages");

                for (int s = 0; s < stages.length(); s++) {
                    JSONObject stageJson = stages.getJSONObject(s);
                    PendingStage stage = new PendingStage(languageName,
                            stageJson.getString("title"), stageJson.getString("level"));

                    JSONArray words = stageJson.getJSONArray("words");
                    for (int w = 0; w < words.length(); w++) {
                        JSONObject word = words.getJSONObject(w);
                        stage.words.add(new String[]{
                                word.getString("word"), word.getString("romanization"),
                                word.getString("meaning"), word.getString("example"),
                                word.getString("exampleMeaning")});
                    }
                    pending.add(stage);
                }
            }

            // Save everything in one transaction: all or nothing, and much faster
            db.runInTransaction(() -> {
                ContentDao dao = db.contentDao();
                for (PendingStage p : pending) {
                    int orderIndex = dao.countStagesForLanguage(p.language); // goes after the existing stages
                    int stageId = (int) dao.insertStage(
                            new StageEntity(p.language, p.title, p.level, orderIndex));

                    List<WordEntity> list = new ArrayList<>();
                    for (String[] w : p.words) {
                        list.add(new WordEntity(stageId, w[0], w[1], w[2], w[3], w[4]));
                    }
                    dao.insertWords(list);
                }
            });
            Log.d(TAG, "Imported pack " + file);
            return true;
        } catch (IOException | JSONException e) {
            Log.e(TAG, "Could not import pack " + file, e);
            return false;
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