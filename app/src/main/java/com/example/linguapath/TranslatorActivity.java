package com.example.linguapath;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;

import com.example.linguapath.data.ReminderSettings;
import com.example.linguapath.data.TranslationRepository;
import com.example.linguapath.data.TranslationUsage;
import com.example.linguapath.databinding.ActivityTranslatorBinding;
import com.example.linguapath.view.Motion;
import com.example.linguapath.data.SavedWordsRepository;
import com.example.linguapath.speech.SpeechHelper;
import com.example.linguapath.system.NetworkMonitor;

public class TranslatorActivity extends AppCompatActivity {

    private static final String STATE_TARGET = "state_target";
    public static final String EXTRA_ADD_WORD = "extra_add_word";

    private ActivityTranslatorBinding binding;
    private String target = "Spanish";
    private String lastSource = "";
    private String lastTranslated = "";
    private SpeechHelper speech;

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
        binding.buttonSpeakResult.setOnClickListener(v -> {
            if (speech != null) {
                speech.speak(lastTranslated, target);
            }
        });
        binding.buttonSave.setOnClickListener(v -> saveWord());

        // Opened from "Add word"? Change the wording.
        if (getIntent().getBooleanExtra(EXTRA_ADD_WORD, false)) {
            binding.textTranslatorTitle.setText(R.string.add_word_title);
            binding.textTranslatorSubtitle.setText(R.string.add_word_subtitle);
        }

        showChips();
        showUsage();

        // Offline: new phrases are queued. Online again: translating is allowed again.
        NetworkMonitor.get(this).online().observe(this, online -> {
            CharSequence note = getString(R.string.translator_offline_note);
            if (!online) {
                binding.textError.setText(note);
            } else if (note.toString().contentEquals(binding.textError.getText())) {
                binding.textError.setText("");
            }
        });
    }
    @Override
    protected void onStart() {
        super.onStart();
        speech = new SpeechHelper(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (speech != null) {
            speech.shutdown();
            speech = null;
        }
    }
    // Saves the translated word into the user's vocabulary (through the Content Provider)
    private void saveWord() {
        if (lastTranslated.isEmpty()) {
            return;
        }
        binding.buttonSave.setEnabled(false);
        SavedWordsRepository.saveCustom(this, target, lastTranslated, lastSource, saved -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            binding.buttonSave.setEnabled(true);
            binding.textResultSource.setText(
                    saved ? R.string.translator_saved : R.string.translator_already_saved);
        });
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
                showUsage();
                if (messageResId == R.string.translator_quota) {
                    showQuotaDialog(); // a popup, and the day is blocked
                } else {
                    binding.textError.setText(messageResId);
                }
            }
        });
    }

    private void showQuotaDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.quota_dialog_title)
                .setMessage(R.string.quota_dialog_message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
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