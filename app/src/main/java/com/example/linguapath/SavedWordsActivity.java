package com.example.linguapath;

import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.linguapath.adapter.SavedWordAdapter;
import com.example.linguapath.data.SavedWordsRepository;
import com.example.linguapath.databinding.ActivitySavedWordsBinding;
import com.example.linguapath.model.SavedWord;
import com.example.linguapath.provider.SavedWordsContract;

public class SavedWordsActivity extends AppCompatActivity {

    private static final String TAG = "SavedWords";

    private ActivitySavedWordsBinding binding;
    private SavedWordAdapter adapter;

    // The provider shouts "something changed!". We reload the list when we hear it.
    private final ContentObserver observer = new ContentObserver(new Handler(Looper.getMainLooper())) {
        @Override
        public void onChange(boolean selfChange) {
            Log.d(TAG, "Provider says the data changed");
            reload();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySavedWordsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.buttonBack.setOnClickListener(v -> finish());

        adapter = new SavedWordAdapter(new SavedWordAdapter.Listener() {
            @Override
            public void onOpen(SavedWord word) {
                openDetail(word);
            }

            @Override
            public void onDelete(SavedWord word) {
                // The list updates by itself when the provider reports the change
                SavedWordsRepository.removeById(SavedWordsActivity.this, word.getId(), removed -> { });
            }
        });
        binding.recyclerSaved.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerSaved.setAdapter(adapter);

        // Demo for your report: the provider tells us its MIME types
        Log.d(TAG, "MIME of the list: " + getContentResolver().getType(SavedWordsContract.CONTENT_URI));
        Uri oneRow = Uri.withAppendedPath(SavedWordsContract.CONTENT_URI, "1");
        Log.d(TAG, "MIME of one row: " + getContentResolver().getType(oneRow));
    }

    // Listen while the screen is visible, stop when it is not
    @Override
    protected void onStart() {
        super.onStart();
        reload();
        getContentResolver().registerContentObserver(SavedWordsContract.CONTENT_URI, true, observer);
    }

    @Override
    protected void onStop() {
        super.onStop();
        getContentResolver().unregisterContentObserver(observer);
    }

    private void reload() {
        SavedWordsRepository.loadAll(this, words -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            adapter.setItems(words);
            binding.textCount.setText(getString(R.string.saved_count, words.size()));
            binding.textEmpty.setVisibility(words.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    // Explicit Intent: open the word's detail screen
    private void openDetail(SavedWord word) {
        Intent intent = new Intent(this, WordDetailActivity.class);
        intent.putExtra(WordDetailActivity.EXTRA_WORD_ID, word.getWordId());
        intent.putExtra(WordDetailActivity.EXTRA_LANGUAGE, word.getLanguage());
        startActivity(intent);
    }
}