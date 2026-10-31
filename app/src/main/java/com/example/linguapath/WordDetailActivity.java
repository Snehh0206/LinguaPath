package com.example.linguapath;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.linguapath.data.WordRepository;
import com.example.linguapath.databinding.ActivityWordDetailBinding;
import com.example.linguapath.model.Word;

public class WordDetailActivity extends AppCompatActivity {

    public static final String EXTRA_WORD_ID = "extra_word_id";
    private static final String TAG = "WordDetail";

    private ActivityWordDetailBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");

        binding = ActivityWordDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.buttonBack.setOnClickListener(v -> finish());

        // The word id arrives in the Intent extras
        int wordId = getIntent().getIntExtra(EXTRA_WORD_ID, -1);

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
            // No app can handle it: tell the user instead of crashing
            binding.textDetailMessage.setText(R.string.share_no_app);
        }
    }
}