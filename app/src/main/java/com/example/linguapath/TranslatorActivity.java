package com.example.linguapath;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.appcompat.app.AppCompatActivity;

import com.example.linguapath.data.ReminderSettings;
import com.example.linguapath.data.TranslationRepository;
import com.example.linguapath.data.TranslationUsage;
import com.example.linguapath.databinding.ActivityTranslatorBinding;
import com.example.linguapath.view.Motion;

public class TranslatorActivity extends AppCompatActivity {

    private static final String STATE_TARGET = "state_target";

    private ActivityTranslatorBinding binding;
    private String target = "Spanish";
    private String lastSource = "";
    private String lastTranslated = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTranslatorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Start with the language studied last. After rotation, keep the user's choice.
        if (savedInstanceState != null) {
            target = savedInstanceState.getString(STATE_TARGET, "Spanish");
        } else {
            String last = new ReminderSettings(this).getLastLanguage();
            if (last != null) {
                target = last;
            }
        }

        binding.buttonBack.setOnClickListener(v -> finish());
        binding.chipKorean.setOnClickListener(v -> selectTarget("Korean"));
        binding.chipSpanish.setOnClickListener(v -> selectTarget("Spanish"));
        binding.buttonTranslate.setOnClickListener(v -> translate());
        binding.buttonCopy.setOnClickListener(v -> copyResult());

        showChips();
        showUsage();
    }

    // Keep the chosen language when the screen is rotated.
    // (The typed text is restored by the EditText itself.)
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_TARGET, target);
    }

    private void selectTarget(String language) {
        target = language;
        showChips();
    }

    private void showChips() {
        boolean korean = "Korean".equals(target);
        binding.chipKorean.setBackgroundResource(korean ? R.drawable.bg_card_sage : R.drawable.bg_card_sky);
        binding.chipSpanish.setBackgroundResource(korean ? R.drawable.bg_card_sky : R.drawable.bg_card_sage);
    }

    private void showUsage() {
        int used = new TranslationUsage(this).getUsedToday();
        binding.textUsage.setText(getString(R.string.translator_usage, used,
                TranslationUsage.DAILY_CHAR_LIMIT));
    }

    private void translate() {
        String text = binding.editText.getText() == null
                ? "" : binding.editText.getText().toString().trim();

        binding.textError.setText("");
        if (text.isEmpty()) {
            binding.textError.setText(R.string.translator_empty);
            return;
        }

        hideKeyboard();
        binding.cardResult.setVisibility(View.GONE);
        setLoading(true);
        lastSource = text;

        TranslationRepository.translate(this, text, target, new TranslationRepository.Callback() {
            @Override
            public void onSuccess(String translated, boolean fromCache) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                setLoading(false);
                showResult(translated, fromCache);
            }

            @Override
            public void onError(int messageResId) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                setLoading(false);
                binding.textError.setText(messageResId);
                showUsage();
            }
        });
    }

    private void showResult(String translated, boolean fromCache) {
        lastTranslated = translated;
        binding.textResult.setText(translated);
        binding.textResultSource.setText(
                fromCache ? R.string.translator_from_cache : R.string.translator_from_online);
        binding.cardResult.setVisibility(View.VISIBLE);
        Motion.fadeSlideIn(binding.cardResult, 0);
        showUsage();
    }

    // While waiting: show the spinner and block extra taps
    private void setLoading(boolean loading) {
        binding.progressTranslate.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.buttonTranslate.setEnabled(!loading);
    }

    private void copyResult() {
        ClipboardManager clipboard = getSystemService(ClipboardManager.class);
        clipboard.setPrimaryClip(ClipData.newPlainText("translation", lastTranslated));
        binding.textResultSource.setText(R.string.translator_copied);
    }

    private void hideKeyboard() {
        InputMethodManager imm = getSystemService(InputMethodManager.class);
        imm.hideSoftInputFromWindow(binding.editText.getWindowToken(), 0);
    }
}