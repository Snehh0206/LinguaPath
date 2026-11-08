package com.example.linguapath.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.linguapath.databinding.ItemSavedWordBinding;
import com.example.linguapath.model.SavedWord;
import com.example.linguapath.view.TouchBounce;

import java.util.ArrayList;
import java.util.List;

// Data -> Adapter -> ViewHolder -> RecyclerView
public class SavedWordAdapter extends RecyclerView.Adapter<SavedWordAdapter.SavedViewHolder> {

    public interface Listener {
        void onOpen(SavedWord word);

        void onDelete(SavedWord word);
    }

    private final List<SavedWord> items = new ArrayList<>();
    private final Listener listener;

    public SavedWordAdapter(Listener listener) {
        this.listener = listener;
    }

    // DiffUtil finds what changed, so rows slide in and out instead of the list flashing
    public void setItems(List<SavedWord> newItems) {
        List<SavedWord> oldItems = new ArrayList<>(items);
        DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return oldItems.size();
            }

            @Override
            public int getNewListSize() {
                return newItems.size();
            }

            @Override
            public boolean areItemsTheSame(int oldPos, int newPos) {
                return oldItems.get(oldPos).getId() == newItems.get(newPos).getId();
            }

            @Override
            public boolean areContentsTheSame(int oldPos, int newPos) {
                return oldItems.get(oldPos).getId() == newItems.get(newPos).getId();
            }
        });
        items.clear();
        items.addAll(newItems);
        result.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public SavedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSavedWordBinding binding = ItemSavedWordBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new SavedViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull SavedViewHolder holder, int position) {
        SavedWord word = items.get(position);
        ItemSavedWordBinding b = holder.binding;

        holder.itemView.animate().cancel();
        holder.itemView.setScaleX(1f);
        holder.itemView.setScaleY(1f);

        b.textSavedWord.setText(word.getWord());
        b.textSavedMeaning.setText(word.getMeaning());
        b.textSavedLanguage.setText(word.getLanguage());

        b.getRoot().setOnClickListener(v -> listener.onOpen(word));
        b.buttonDelete.setOnClickListener(v -> listener.onDelete(word));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class SavedViewHolder extends RecyclerView.ViewHolder {
        final ItemSavedWordBinding binding;

        SavedViewHolder(ItemSavedWordBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            TouchBounce.attach(binding.getRoot());
        }
    }
}