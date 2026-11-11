package com.example.linguapath.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class AvatarStore {

    private static File file(Context c, String uid) {
        return new File(c.getFilesDir(), "avatar_" + uid + ".jpg");
    }

    public static Bitmap load(Context c, String uid) {
        File f = file(c, uid);
        return f.exists() ? BitmapFactory.decodeFile(f.getPath()) : null;
    }

    public static void delete(Context c, String uid) {
        file(c, uid).delete();
    }

    // Call from a background thread. Crops to a square and shrinks to 256 px.
    public static boolean save(Context c, String uid, Uri uri) {
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            try (InputStream in = c.getContentResolver().openInputStream(uri)) {
                BitmapFactory.decodeStream(in, null, o);
            }
            int side = Math.min(o.outWidth, o.outHeight);
            if (side <= 0) {
                return false;
            }
            o.inJustDecodeBounds = false;
            o.inSampleSize = Math.max(1, side / 512);
            Bitmap bmp;
            try (InputStream in = c.getContentResolver().openInputStream(uri)) {
                bmp = BitmapFactory.decodeStream(in, null, o);
            }
            if (bmp == null) {
                return false;
            }

            int rotation = 0;
            try (InputStream in = c.getContentResolver().openInputStream(uri)) {
                int ori = new ExifInterface(in).getAttributeInt(
                        ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                if (ori == ExifInterface.ORIENTATION_ROTATE_90) rotation = 90;
                else if (ori == ExifInterface.ORIENTATION_ROTATE_180) rotation = 180;
                else if (ori == ExifInterface.ORIENTATION_ROTATE_270) rotation = 270;
            } catch (Exception ignored) {
                // no rotation info
            }

            int s = Math.min(bmp.getWidth(), bmp.getHeight());
            Matrix m = new Matrix();
            m.postRotate(rotation);
            Bitmap square = Bitmap.createBitmap(bmp, (bmp.getWidth() - s) / 2,
                    (bmp.getHeight() - s) / 2, s, s, m, true);
            Bitmap out = Bitmap.createScaledBitmap(square, 256, 256, true);
            try (FileOutputStream fo = new FileOutputStream(file(c, uid))) {
                out.compress(Bitmap.CompressFormat.JPEG, 90, fo);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}