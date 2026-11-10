package com.example.linguapath.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.linguapath.R;
import com.example.linguapath.databinding.ItemTopicBinding;
import com.example.linguapath.model.Topic;
import com.example.linguapath.view.TouchBounce;

import java.util.ArrayList;
import java.util.List;

// Topic cards in a grid
public class TopicAdapter extends RecyclerView.Adapter<TopicAdapter.TopicViewHolder> {

    public interface Listener {
        void onTopic(Topic topic);
    }

    // The card color follows the topic's theme
    private static final int[] CARD_BG = {
            R.drawable.bg_card_sage, R.drawable.bg_card_butter, R.drawable.bg_card_sky,
            R.drawable.bg_card_peach, R.drawable.bg_card_peach, R.drawable.bg_card_sage,
            R.drawable.bg_card_sky};

    private final List<Topic> topics = new ArrayList<>();
    private final Listener listener;
    private String language = "Spanish";

    public TopicAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setData(List<Topic> newTopics, String language) {
        this.language = language;
        topics.clear();
        topics.addAll(newTopics);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TopicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemTopicBinding binding = ItemTopicBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new TopicViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull TopicViewHolder holder, int position) {
        Topic topic = topics.get(position);
        ItemTopicBinding b = holder.binding;

        holder.itemView.animate().cancel();
        holder.itemView.setScaleX(1f);
        holder.itemView.setScaleY(1f);

        b.textTopicEmoji.setText(topic.emoji);
        b.textTopicTitle.setText(topic.title);
        b.textTopicCount.setText(b.getRoot().getContext()
                .getString(R.string.topic_word_count, topic.getWords(language).size()));
        b.getRoot().setBackgroundResource(CARD_BG[Math.min(topic.theme, CARD_BG.length - 1)]);
        b.getRoot().setOnClickListener(v -> listener.onTopic(topic));
    }

    @Override
    public int getItemCount() {
        return topics.size();
    }

    static class TopicViewHolder extends RecyclerView.ViewHolder {
        final ItemTopicBinding binding;

        TopicViewHolder(ItemTopicBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            TouchBounce.attach(binding.getRoot());
        }
    }
}