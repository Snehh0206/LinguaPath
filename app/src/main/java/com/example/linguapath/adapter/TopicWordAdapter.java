package com.example.linguapath.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.linguapath.databinding.ItemTopicWordBinding;
import com.example.linguapath.model.TopicWord;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import com.example.linguapath.R;

import java.util.HashSet;
import java.util.Set;

public class TopicWordAdapter extends RecyclerView.Adapter<TopicWordAdapter.TopicWordViewHolder> {

    private final List<TopicWord> words = new ArrayList<>();
    private Consumer<TopicWord> speakListener;
    private Consumer<TopicWord> saveListener;

    public void setSpeakListener(Consumer<TopicWord> listener) {
        this.speakListener = listener;
    }

    public void setSaveListener(Consumer<TopicWord> listener) {
        this.saveListener = listener;
    }

    public void setWords(List<TopicWord> newWords) {
        words.clear();
        words.addAll(newWords);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TopicWordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemTopicWordBinding binding = ItemTopicWordBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new TopicWordViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull TopicWordViewHolder holder, int position) {
        TopicWord word = words.get(position);
        ItemTopicWordBinding b = holder.binding;

        b.textWordEmoji.setText(word.emoji);
        b.textWord.setText(word.word);
        boolean hidden = learnMode && !revealed.contains(word.word);
        b.textMeaning.setText(hidden
                ? b.getRoot().getContext().getString(R.string.learn_tap_to_reveal)
                : word.meaning);
        b.textMeaning.setAlpha(hidden ? 0.5f : 1f);
        b.getRoot().setRotationX(0f);
        b.getRoot().setOnClickListener(v -> {
            if (learnMode && !revealed.contains(word.word)) {
                revealed.add(word.word);
                flip(b, word);
            }
        });

        // Rows get recycled: set the visibility every time
        if (word.romanization.isEmpty()) {
            b.textRomanization.setVisibility(View.GONE);
        } else {
            b.textRomanization.setVisibility(View.VISIBLE);
            b.textRomanization.setText(word.romanization);
        }

        b.buttonSpeak.setOnClickListener(v -> {
            if (speakListener != null) {
                speakListener.accept(word);
            }
        });
        b.buttonSave.setOnClickListener(v -> {
            if (saveListener != null) {
                saveListener.accept(word);
            }
        });
    }
    // The card turns away, the meaning appears, and it turns back
    private void flip(ItemTopicWordBinding b, TopicWord word) {
        View card = b.getRoot();
        card.animate().rotationX(90f).setDuration(150).withEndAction(() -> {
            b.textMeaning.setText(word.meaning);
            b.textMeaning.setAlpha(1f);
            card.setRotationX(-90f);
            card.animate().rotationX(0f).setDuration(150);
        });
    }
    @Override
    public int getItemCount() {
        return words.size();
    }
    private boolean learnMode = false;
    private final Set<String> revealed = new HashSet<>();

    // In learn mode the meaning is hidden until the card is tapped
    public void setLearnMode(boolean on) {
        learnMode = on;
        revealed.clear();
        notifyDataSetChanged();
    }
    static class TopicWordViewHolder extends RecyclerView.ViewHolder {
        final ItemTopicWordBinding binding;

        TopicWordViewHolder(ItemTopicWordBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}