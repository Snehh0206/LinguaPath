package com.example.linguapath;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.format.Formatter;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.linguapath.data.ImageLoader;
import com.example.linguapath.data.ReminderSettings;
import com.example.linguapath.data.TranslationRepository;
import com.example.linguapath.databinding.ActivitySignTranslateBinding;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.content.ActivityNotFoundException;
import androidx.core.content.FileProvider;
import java.io.File;

// Gallery photo of a sign -> text recognition (on the phone) -> translation to English.
// The photo comes through the system Photo Picker, as a content:// address that we read with ContentResolver.
public class SignTranslateActivity extends AppCompatActivity {

    private static final String STATE_SOURCE = "state_source";
    private static final int MAX_PHOTO_SIDE = 1600;
    private static final int MAX_TRANSLATE_BYTES = 450; // MyMemory takes at most 500 bytes

    private static final ExecutorService photoExecutor = Executors.newSingleThreadExecutor();

    private ActivitySignTranslateBinding binding;
    private ActivityResultLauncher<PickVisualMediaRequest> pickPhoto;
    private TextRecognizer latinRecognizer;
    private TextRecognizer koreanRecognizer;
    private Bitmap currentBitmap;
    private static final String STATE_CAMERA_URI = "state_camera_uri";
    private ActivityResultLauncher<Uri> takePicture;
    private Uri cameraUri;
    private String source = "Spanish"; // the language written on the sign

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignTranslateBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (savedInstanceState != null) {
            String savedCamera = savedInstanceState.getString(STATE_CAMERA_URI);
            if (savedCamera != null) {
                cameraUri = Uri.parse(savedCamera);
            }
            source = savedInstanceState.getString(STATE_SOURCE, "Spanish");
        } else {
            String last = new ReminderSettings(this).getLastLanguage();
            if (last != null) {
                source = last;
            }
        }

        // The system Photo Picker: no permission needed. It returns a content:// address.
        pickPhoto = registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
            if (uri != null) {
                onPhotoPicked(uri);
            }
        });
        // The camera app saves the photo to the address we give it
        takePicture = registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
            if (success && cameraUri != null) {
                onPhotoPicked(cameraUri); // same path as a gallery photo
            }
        });
        binding.buttonTakePhoto.setOnClickListener(v -> openCamera());

        binding.buttonBack.setOnClickListener(v -> finish());
        binding.chipKorean.setOnClickListener(v -> selectSource("Korean"));
        binding.chipSpanish.setOnClickListener(v -> selectSource("Spanish"));
        binding.buttonChoosePhoto.setOnClickListener(v -> pickPhoto.launch(
                new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build()));
        binding.buttonTranslate.setOnClickListener(v -> translate());
        showChips();
    }
    // Opens the phone's camera app. The photo is saved through a FileProvider (a content provider),
    // so the app needs no camera permission.
    private void openCamera() {
        try {
            File folder = new File(getCacheDir(), "camera");
            if (!folder.exists() && !folder.mkdirs()) {
                showError(R.string.sign_read_failed);
                return;
            }
            File file = new File(folder, "sign_" + System.currentTimeMillis() + ".jpg");
            cameraUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            takePicture.launch(cameraUri);
        } catch (ActivityNotFoundException | IllegalArgumentException e) {
            showError(R.string.sign_no_camera);
        }
    }
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_SOURCE, source);
        if (cameraUri != null) {
            outState.putString(STATE_CAMERA_URI, cameraUri.toString());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (latinRecognizer != null) {
            latinRecognizer.close();
        }
        if (koreanRecognizer != null) {
            koreanRecognizer.close();
        }
    }

    private void selectSource(String language) {
        source = language;
        showChips();
        if (currentBitmap != null) {
            recognize(currentBitmap); // read the same photo again with the other language
        }
    }

    private void showChips() {
        boolean korean = "Korean".equals(source);
        binding.chipKorean.setBackgroundResource(korean ? R.drawable.bg_card_sage : R.drawable.bg_card_sky);
        binding.chipSpanish.setBackgroundResource(korean ? R.drawable.bg_card_sky : R.drawable.bg_card_sage);
    }

    // ---- 1. The photo ----

    private void onPhotoPicked(Uri uri) {
        binding.textMessage.setText("");
        binding.cardResult.setVisibility(View.GONE);
        showPhotoInfo(uri);

        setBusy(true);
        photoExecutor.execute(() -> {
            try {
                Bitmap bitmap = ImageLoader.decode(this, uri, MAX_PHOTO_SIDE);
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    currentBitmap = bitmap;
                    binding.imageSign.setImageBitmap(bitmap);
                    binding.imageSign.setVisibility(View.VISIBLE);
                    recognize(bitmap);
                });
            } catch (IOException | RuntimeException e) {
                runOnUiThread(() -> {
                    setBusy(false);
                    binding.textMessage.setText(R.string.sign_read_failed);
                });
            }
        });
    }

    // Asks the content provider for the picture's name and size (ContentResolver query)
    private void showPhotoInfo(Uri uri) {
        String name = "";
        long size = 0;
        try (Cursor cursor = getContentResolver().query(uri,
                new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                name = cursor.getString(0);
                size = cursor.getLong(1);
            }
        }
        binding.textPhotoInfo.setText(getString(R.string.sign_photo_info,
                name == null ? "" : name, Formatter.formatShortFileSize(this, size)));
    }

    // ---- 2. Text recognition (on the phone) ----

    private TextRecognizer recognizerFor(String language) {
        if ("Korean".equals(language)) {
            if (koreanRecognizer == null) {
                koreanRecognizer = TextRecognition.getClient(new KoreanTextRecognizerOptions.Builder().build());
            }
            return koreanRecognizer;
        }
        if (latinRecognizer == null) {
            latinRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        }
        return latinRecognizer;
    }

    private void recognize(Bitmap bitmap) {
        setBusy(true);
        binding.textMessage.setText(R.string.sign_reading);
        binding.textMessage.setTextColor(getColor(R.color.ink_soft));

        recognizerFor(source).process(InputImage.fromBitmap(bitmap, 0))
                .addOnSuccessListener(result -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    setBusy(false);
                    String text = result.getText().trim();
                    binding.editRecognized.setText(text);
                    if (text.isEmpty()) {
                        showError(R.string.sign_no_text);
                    } else {
                        binding.textMessage.setText("");
                        translate(); // translate straight away. The user can still fix the text and translate again.
                    }
                })
                .addOnFailureListener(e -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    setBusy(false);
                    showError(R.string.sign_read_failed);
                });
    }

    // ---- 3. Translation ----

    private void translate() {
        String text = binding.editRecognized.getText() == null
                ? "" : binding.editRecognized.getText().toString().replaceAll("\\s+", " ").trim();
        if (text.isEmpty()) {
            showError(R.string.sign_no_text);
            return;
        }
        String shortened = truncateBytes(text, MAX_TRANSLATE_BYTES);
        boolean trimmed = shortened.length() < text.length();

        setBusy(true);
        binding.cardResult.setVisibility(View.GONE);
        TranslationRepository.translateToEnglish(this, shortened, source, new TranslationRepository.Callback() {
            @Override
            public void onSuccess(String translated, boolean fromCache) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                setBusy(false);
                binding.textMessage.setText(trimmed ? getString(R.string.sign_trimmed) : "");
                binding.textMessage.setTextColor(getColor(R.color.ink_soft));
                binding.textResult.setText(translated);
                binding.textResultSource.setText(
                        fromCache ? R.string.translator_from_cache : R.string.translator_from_online);
                binding.cardResult.setVisibility(View.VISIBLE);
            }

            @Override
            public void onError(int messageResId) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                setBusy(false);
                if (messageResId == R.string.translator_quota) {
                    new AlertDialog.Builder(SignTranslateActivity.this)
                            .setTitle(R.string.quota_dialog_title)
                            .setMessage(R.string.quota_dialog_message)
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                } else {
                    showError(messageResId); // offline: kept and translated when we are back online
                }
            }
        });
    }

    // Cuts the text to a number of bytes without cutting a character in half
    private static String truncateBytes(String text, int maxBytes) {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        if (bytes.length <= maxBytes) {
            return text;
        }
        int end = maxBytes;
        while (end > 0 && (bytes[end] & 0xC0) == 0x80) {
            end--;
        }
        return new String(bytes, 0, end, StandardCharsets.UTF_8);
    }

    private void showError(int messageRes) {
        binding.textMessage.setTextColor(getColor(R.color.error));
        binding.textMessage.setText(messageRes);
    }

    private void setBusy(boolean busy) {
        binding.progressSign.setVisibility(busy ? View.VISIBLE : View.GONE);
        binding.buttonTranslate.setEnabled(!busy);
        binding.buttonChoosePhoto.setEnabled(!busy);
        binding.buttonTakePhoto.setEnabled(!busy);
    }
}