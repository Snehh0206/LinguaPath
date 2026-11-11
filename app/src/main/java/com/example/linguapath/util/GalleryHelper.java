package com.example.linguapath.util;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.provider.Settings;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.linguapath.R;

// Opens the phone's own gallery app (not the Google Photos picker) after asking for photo access.
// Create it in onCreate, then call open().
public class GalleryHelper {

    public interface Callback {
        void onPicked(Uri uri);
    }

    private final AppCompatActivity activity;
    private final ActivityResultLauncher<String[]> askAccess;
    private final ActivityResultLauncher<Intent> pickImage;

    public GalleryHelper(AppCompatActivity activity, Callback callback) {
        this.activity = activity;
        askAccess = activity.registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                    if (hasAccess()) {
                        openGallery();
                    } else if (!activity.shouldShowRequestPermissionRationale(permissions()[0])) {
                        showSettingsDialog();
                    } else {
                        Toast.makeText(activity, R.string.gallery_denied_soft,
                                Toast.LENGTH_SHORT).show();
                    }
                });
        pickImage = activity.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    Intent data = result.getData();
                    if (result.getResultCode() == android.app.Activity.RESULT_OK
                            && data != null && data.getData() != null) {
                        callback.onPicked(data.getData());
                    }
                });
    }

    private static String[] permissions() {
        if (Build.VERSION.SDK_INT >= 34) {
            return new String[]{Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED};
        }
        if (Build.VERSION.SDK_INT >= 33) {
            return new String[]{Manifest.permission.READ_MEDIA_IMAGES};
        }
        return new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
    }

    private boolean hasAccess() {
        for (String p : permissions()) {
            if (ContextCompat.checkSelfPermission(activity, p)
                    == PackageManager.PERMISSION_GRANTED) {
                return true; // full access, or "selected photos" on Android 14+
            }
        }
        return false;
    }

    // Checks the permission every time, so turning it off in Settings asks again
    public void open() {
        if (hasAccess()) {
            openGallery();
            return;
        }
        new AlertDialog.Builder(activity)
                .setTitle(R.string.gallery_why_title)
                .setMessage(R.string.gallery_why_message)
                .setPositiveButton(R.string.reminder_why_continue,
                        (d, w) -> askAccess.launch(permissions()))
                .setNegativeButton(R.string.reminder_not_now, null)
                .show();
    }

    private void openGallery() {
        Intent pick = new Intent(Intent.ACTION_PICK);
        pick.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        try {
            pickImage.launch(pick);
        } catch (ActivityNotFoundException e) {
            Intent get = new Intent(Intent.ACTION_GET_CONTENT);
            get.setType("image/*");
            try {
                pickImage.launch(get);
            } catch (ActivityNotFoundException e2) {
                Toast.makeText(activity, R.string.sign_read_failed, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showSettingsDialog() {
        new AlertDialog.Builder(activity)
                .setTitle(R.string.reminder_settings_title_perm)
                .setMessage(R.string.perm_settings_photos)
                .setPositiveButton(R.string.reminder_open_settings, (d, w) -> {
                    Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", activity.getPackageName(), null));
                    activity.startActivity(i);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}