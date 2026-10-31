package com.example.linguapath.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.linguapath.databinding.ItemWordBinding;
import com.example.linguapath.model.Word;

import java.util.ArrayList;
import java.util.List;

public class WordAdapter extends RecyclerView.Adapter<WordAdapter.WordViewHolder> {

    public interface OnWordClickListener {
        void onWordClick(Word word);
    }

    private final List<Word> words = new ArrayList<>();
    private final OnWordClickListener listener;

    public WordAdapter(OnWordClickListener listener) {
        this.listener = listener;
    }

    public void setWords(List<Word> newWords) {
        words.clear();
        words.addAll(newWords);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public WordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemWordBinding binding = ItemWordBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new WordViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull WordViewHolder holder, int position) {
        Word word = words.get(position);
        ItemWordBinding b = holder.binding;

        b.textWord.setText(word.getWord());
        b.textMeaning.setText(word.getMeaning());

        // Spanish has no romanization. Set visibility every time, because rows get recycled.
        if (word.getRomanization().isEmpty()) {
            b.textRomanization.setVisibility(View.GONE);
        } else {
            b.textRomanization.setVisibility(View.VISIBLE);
            b.textRomanization.setText(word.getRomanization());
        }

        b.getRoot().setOnClickListener(v -> listener.onWordClick(word));
    }

    @Override
    public int getItemCount() {
        return words.size();
    }

    static class WordViewHolder extends RecyclerView.ViewHolder {
        final ItemWordBinding binding;

        WordViewHolder(ItemWordBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}