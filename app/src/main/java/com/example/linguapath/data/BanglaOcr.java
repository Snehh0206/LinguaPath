package com.example.linguapath.data;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

// Reads Bengali text from a photo with Tesseract (free, offline).
// ML Kit cannot read Bengali, so this is only used for Bangla.
// Call it on a background thread.
public class BanglaOcr {

    private static final String TAG = "BanglaOcr";
    private static final String LANG = "ben";

    // Returns the text ("" if none was found), or null if something went wrong
    public static synchronized String read(Context context, Bitmap bitmap) {
        TessBaseAPI api = null;
        try {
            File dir = new File(context.getFilesDir(), "tessdata");
            File data = new File(dir, LANG + ".traineddata");
            if (!data.exists()) {
                copyFromAssets(context, dir, data);
            }
            // Tesseract wants the folder that CONTAINS the "tessdata" folder
            api = new TessBaseAPI();
            if (!api.init(context.getFilesDir().getAbsolutePath(), LANG)) {
                Log.e(TAG, "init failed");
                return null;
            }
            Bitmap copy = bitmap.copy(Bitmap.Config.ARGB_8888, false);
            api.setImage(copy);
            String text = api.getUTF8Text();
            return text == null ? "" : text.trim();
        } catch (Exception e) {
            Log.e(TAG, "Could not read the photo", e);
            return null;
        } finally {
            if (api != null) {
                api.recycle();
            }
        }
    }

    private static void copyFromAssets(Context context, File dir, File target) throws Exception {
        if (!dir.exists() && !dir.mkdirs()) {
            throw new java.io.IOException("Cannot create " + dir);
        }
        try (InputStream in = context.getAssets().open("tessdata/" + LANG + ".traineddata");
             OutputStream out = new FileOutputStream(target)) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = in.read(buffer)) != -1) {
                out.write(buffer, 0, count);
            }
        }
    }
}