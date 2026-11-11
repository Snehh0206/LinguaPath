package com.example.linguapath;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.linguapath.data.ReminderSettings;
import com.example.linguapath.data.SavedWordsRepository;
import com.example.linguapath.data.TranslationRepository;
import com.example.linguapath.data.TranslationUsage;
import com.example.linguapath.databinding.ActivityTranslatorBinding;
import com.example.linguapath.speech.SpeechHelper;
import com.example.linguapath.system.NetworkMonitor;
import com.example.linguapath.view.Motion;

import java.util.ArrayList;

public class TranslatorActivity extends AppCompatActivity {

    private static final String STATE_TARGET = "state_target";
    private static final String STATE_TO_ENGLISH = "state_to_english";
    public static final String EXTRA_ADD_WORD = "extra_add_word";

    private ActivityTranslatorBinding binding;
    private String target = "Spanish";   // the foreign language (Korean or Spanish)
    private boolean toEnglish = false;   // false: English -> foreign, true: foreign -> English
    private String lastSource = "";
    private String lastTranslated = "";
    private SpeechHelper speech;
    private ActivityResultLauncher<Intent> voiceLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTranslatorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (savedInstanceState != null) {
            target = savedInstanceState.getString(STATE_TARGET, "Spanish");
            toEnglish = savedInstanceState.getBoolean(STATE_TO_ENGLISH, false);
        } else {
            String last = new ReminderSettings(this).getLastLanguage();
            if (last != null) {
                target = last;
            }
        }

        // The phone's own speech screen listens and returns text. No extra permission needed.
        voiceLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() != RESULT_OK || result.getData() == null) {
                        return;
                    }
                    ArrayList<String> heard =
                            result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    if (heard != null && !heard.isEmpty()) {
                        binding.editText.setText(heard.get(0));
                        translate(); // written text first, then it can be spoken with the speaker button
                    }
                });

        binding.buttonBack.setOnClickListener(v -> finish());
        binding.chipKorean.setOnClickListener(v -> selectTarget("Korean"));
        binding.chipSpanish.setOnClickListener(v -> selectTarget("Spanish"));
        binding.buttonSwap.setOnClickListener(v -> {
            toEnglish = !toEnglish;
            directionChanged();
        });
        binding.buttonMic.setOnClickListener(v -> startVoice());
        binding.buttonTranslate.setOnClickListener(v -> translate());
        binding.buttonCopy.setOnClickListener(v -> copyResult());
        binding.buttonSpeakResult.setOnClickListener(v -> {
            if (speech != null) {
                speech.speak(lastTranslated, toEnglish ? "English" : target);
            }
        });
        binding.buttonSave.setOnClickListener(v -> saveWord());

        if (getIntent().getBooleanExtra(EXTRA_ADD_WORD, false)) {
            binding.textTranslatorTitle.setText(R.string.add_word_title);
            binding.textTranslatorSubtitle.setText(R.string.add_word_subtitle);
        }

        showChips();
        showDirection();
        showUsage();

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

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_TARGET, target);
        outState.putBoolean(STATE_TO_ENGLISH, toEnglish);
    }

    // ---- Language and direction ----

    private void selectTarget(String language) {
        target = language;
        showChips();
        directionChanged();
    }

    private void directionChanged() {
        showDirection();
        binding.cardResult.setVisibility(View.GONE);
        binding.textError.setText("");
    }

    private void showChips() {
        boolean korean = "Korean".equals(target);
        binding.chipKorean.setBackgroundResource(korean ? R.drawable.bg_card_sage : R.drawable.bg_card_sky);
        binding.chipSpanish.setBackgroundResource(korean ? R.drawable.bg_card_sky : R.drawable.bg_card_sage);
    }

    private void showDirection() {
        binding.buttonSwap.setText(getString(
                toEnglish ? R.string.dir_to_english : R.string.dir_from_english, target));
        binding.inputLayout.setHint(toEnglish
                ? getString(R.string.translator_hint_foreign, target)
                : getString(R.string.translator_hint));
    }

    // ---- Voice input ----

    private void startVoice() {
        if (new TranslationUsage(this).isBlockedToday()) {
            showQuotaDialog();
            return;
        }
        String speakTag = toEnglish
                ? ("Korean".equals(target) ? "ko-KR" : "es-ES")
                : "en-US";
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, speakTag);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.translator_mic_prompt));
        try {
            voiceLauncher.launch(intent);
        } catch (ActivityNotFoundException e) {
            binding.textError.setText(R.string.translator_mic_unavailable);
        }
    }

    // ---- Usage, saving, translating ----

    private void showUsage() {
        int used = new TranslationUsage(this).getUsedToday();
        binding.textUsage.setText(getString(R.string.translator_usage, used,
                TranslationUsage.DAILY_CHAR_LIMIT));
    }

    // Saves the word into the user's vocabulary (through the Content Provider)
    private void saveWord() {
        if (lastTranslated.isEmpty()) {
            return;
        }
        String foreign = toEnglish ? lastSource : lastTranslated;
        String english = toEnglish ? lastTranslated : lastSource;
        binding.buttonSave.setEnabled(false);
        SavedWordsRepository.saveCustom(this, target, foreign, english, saved -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            binding.buttonSave.setEnabled(true);
            binding.textResultSource.setText(
                    saved ? R.string.translator_saved : R.string.translator_already_saved);
        });
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

        TranslationRepository.Callback callback = new TranslationRepository.Callback() {
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
        };

        if (toEnglish) {
            TranslationRepository.translateToEnglish(this, text, target, callback);
        } else {
            TranslationRepository.translate(this, text, target, callback);
        }
    }

    private void showQuotaDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.quota_dialog_title)
                .setMessage(R.string.quota_dialog_message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    // A warning a little before the limit, once a day
    private void warnIfNearLimit() {
        TranslationUsage usage = new TranslationUsage(this);
        if (usage.shouldWarnNow()) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.translator_warn_title)
                    .setMessage(getString(R.string.translator_warn_message, usage.getRemaining()))
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        }
    }

    private void showResult(String translated, boolean fromCache) {
        lastTranslated = translated;
        binding.textResult.setText(translated);
        binding.textResultSource.setText(
                fromCache ? R.string.translator_from_cache : R.string.translator_from_online);
        binding.cardResult.setVisibility(View.VISIBLE);
        Motion.fadeSlideIn(binding.cardResult, 0);
        showUsage();
        warnIfNearLimit();
    }

    private void setLoading(boolean loading) {
        binding.progressTranslate.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.buttonTranslate.setEnabled(!loading);
        binding.buttonMic.setEnabled(!loading);
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