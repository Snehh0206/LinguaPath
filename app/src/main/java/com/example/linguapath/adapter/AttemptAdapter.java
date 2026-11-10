package com.example.linguapath.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.linguapath.R;
import com.example.linguapath.data.QuizAttempt;
import com.example.linguapath.databinding.ItemAttemptBinding;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

// Data -> Adapter -> ViewHolder -> RecyclerView: the quiz history list
public class AttemptAdapter extends RecyclerView.Adapter<AttemptAdapter.AttemptViewHolder> {

    private final List<QuizAttempt> items = new ArrayList<>();
    private final DateFormat dateFormat =
            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);

    public void setItems(List<QuizAttempt> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AttemptViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAttemptBinding binding = ItemAttemptBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new AttemptViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull AttemptViewHolder holder, int position) {
        QuizAttempt a = items.get(position);
        ItemAttemptBinding b = holder.binding;
        android.content.Context context = b.getRoot().getContext();

        boolean revision = a.stageIndex == -1;
        boolean topic = a.stageIndex == -2;
        String name = revision ? context.getString(R.string.attempt_revision)
                : topic ? context.getString(R.string.attempt_topic_title, a.stageTitle)
                : a.stageTitle;

        b.textAttemptTitle.setText(context.getString(R.string.attempt_title, name, a.language));
        b.textAttemptDate.setText(dateFormat.format(new Date(a.createdAt)));
        b.textAttemptScore.setText(context.getString(R.string.attempt_score, a.correct, a.total, a.percent));

        int status = revision ? R.string.attempt_revision
                : topic ? R.string.attempt_topic_quiz
                : a.redo ? R.string.attempt_practice
                : a.passed ? R.string.attempt_passed
                : R.string.attempt_not_passed;
        b.textAttemptStatus.setText(status);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class AttemptViewHolder extends RecyclerView.ViewHolder {
        final ItemAttemptBinding binding;

        AttemptViewHolder(ItemAttemptBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}