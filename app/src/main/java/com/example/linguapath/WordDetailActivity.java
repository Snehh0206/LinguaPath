package com.example.linguapath;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.linguapath.data.SavedWordsRepository;
import com.example.linguapath.data.WordRepository;
import com.example.linguapath.databinding.ActivityWordDetailBinding;
import com.example.linguapath.model.Word;
import com.example.linguapath.speech.SpeechHelper;
import com.example.linguapath.service.StudyTimerController;

public class WordDetailActivity extends AppCompatActivity {

    public static final String EXTRA_WORD_ID = "extra_word_id";
    public static final String EXTRA_LANGUAGE = "extra_language";
    private static final String TAG = "WordDetail";

    private ActivityWordDetailBinding binding;
    private String language = "";
    private Word currentWord;
    private boolean saved = false;
    private SpeechHelper speech;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");

        binding = ActivityWordDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.buttonBack.setOnClickListener(v -> finish());

        // The word id and language arrive in the Intent extras
        int wordId = getIntent().getIntExtra(EXTRA_WORD_ID, -1);
        String languageExtra = getIntent().getStringExtra(EXTRA_LANGUAGE);
        if (languageExtra != null) {
            language = languageExtra;
        }

        WordRepository.loadWord(this, wordId, word -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            if (word == null) {
                binding.textDetailMessage.setText(R.string.word_not_found);
                return;
            }
            showWord(word);
        });
    }

    private void showWord(Word word) {
        currentWord = word;
        binding.textDetailWord.setText(word.getWord());
        binding.textDetailMeaning.setText(word.getMeaning());
        binding.textDetailExample.setText(word.getExample());
        binding.textDetailExampleMeaning.setText(word.getExampleMeaning());

        if (word.getRomanization().isEmpty()) {
            binding.textDetailRomanization.setVisibility(View.GONE);
        } else {
            binding.textDetailRomanization.setText(word.getRomanization());
        }

        // The word floats up and fades in
        binding.textDetailWord.setAlpha(0f);
        binding.textDetailWord.setTranslationY(40f);
        binding.textDetailWord.animate().alpha(1f).translationY(0f).setDuration(450);

        binding.buttonShare.setEnabled(true);
        binding.buttonShare.setOnClickListener(v -> shareWord(word));

        // Save button: only possible when we know which language the word belongs to
        if (language.isEmpty()) {
            binding.buttonSave.setVisibility(View.GONE);
        } else {
            binding.buttonSave.setOnClickListener(v -> toggleSaved());
            SavedWordsRepository.isSaved(this, language, word.getWord(), isSaved -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                saved = isSaved;
                updateSaveButton();
            });
        }
        // Pronunciation needs to know the language
        if (language.isEmpty()) {
            binding.buttonSpeakWord.setVisibility(View.GONE);
            binding.buttonSpeakExample.setVisibility(View.GONE);
        } else {
            binding.buttonSpeakWord.setOnClickListener(v -> speak(word.getWord()));
            binding.buttonSpeakExample.setOnClickListener(v -> speak(word.getExample()));
        }
    }

    private void updateSaveButton() {
        binding.buttonSave.setEnabled(true);
        binding.buttonSave.setText(saved ? R.string.saved_word_tap_remove : R.string.save_word);
    }

    // Saves the word, or removes it if it is already saved. Both go through the Content Provider.
    private void toggleSaved() {
        if (currentWord == null) {
            return;
        }
        binding.buttonSave.setEnabled(false); // no double taps while working

        if (saved) {
            SavedWordsRepository.remove(this, language, currentWord.getWord(), removed -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                saved = false;
                updateSaveButton();
                binding.textDetailMessage.setText(R.string.word_removed_message);
            });
        } else {
            SavedWordsRepository.save(this, language, currentWord, savedNow -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                saved = true; // saved now, or it was already there
                updateSaveButton();
                binding.textDetailMessage.setText(R.string.word_saved_message);
            });
        }
    }
    // The speech engine lives only while the screen is visible
    @Override
    protected void onStart() {
        super.onStart();
        speech = new SpeechHelper(this);
        StudyTimerController.screenStarted();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (speech != null) {
            speech.shutdown();
            speech = null;
        }
        StudyTimerController.screenStopped();
    }

    private void speak(String text) {
        if (speech != null) {
            speech.speak(text, language);
        }
    }
    // Implicit Intent: we don't name an app, we say "send this text" and Android lets the user pick
    private void shareWord(Word word) {
        String text = word.getWord();
        if (!word.getRomanization().isEmpty()) {
            text += " (" + word.getRomanization() + ")";
        }
        text += " = " + word.getMeaning()
                + "\n" + word.getExample()
                + "\nShared from LinguaPath";

        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, text);

        try {
            startActivity(Intent.createChooser(send, getString(R.string.share_chooser_title)));
        } catch (ActivityNotFoundException e) {
            binding.textDetailMessage.setText(R.string.share_no_app);
        }
    }
}