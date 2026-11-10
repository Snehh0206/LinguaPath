package com.example.linguapath.adapter;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.linguapath.R;
import com.example.linguapath.databinding.ItemStageBinding;
import com.example.linguapath.model.Stage;
import com.example.linguapath.view.RopeView;

import java.util.List;
import com.example.linguapath.view.TouchBounce;
import java.util.HashMap;
import java.util.Map;

public class StageAdapter extends RecyclerView.Adapter<StageAdapter.StageViewHolder> {

    public static final int STATE_DONE = 0;
    public static final int STATE_CURRENT = 1;
    public static final int STATE_LOCKED = 2;

    public interface OnStageClickListener {
        void onStageClick(Stage stage, int state);
    }

    // Left, middle, right, middle... makes the path wind
    private static final int[] GRAVITY_PATTERN = {
            Gravity.START, Gravity.CENTER_HORIZONTAL, Gravity.END, Gravity.CENTER_HORIZONTAL
    };

    // Same pattern, in the form RopeView understands
    private static final int[] ROPE_PATTERN = {
            RopeView.LEFT, RopeView.CENTER, RopeView.RIGHT, RopeView.CENTER
    };

    private  List<Stage> stages;
    private final OnStageClickListener listener;
    private int completedCount = 0;

    public StageAdapter(List<Stage> stages, OnStageClickListener listener) {
        this.stages = stages;
        this.listener = listener;
    }

    public void setCompletedCount(int completedCount) {
        this.completedCount = completedCount;
        notifyDataSetChanged();
    }
    // Called when the stages arrive from the database
    public void setStages(List<Stage> stages) {
        this.stages = stages;
        notifyDataSetChanged();
    }
    private int getState(int position) {
        if (position < completedCount) return STATE_DONE;
        if (position == completedCount) return STATE_CURRENT;
        return STATE_LOCKED;
    }

    @NonNull
    @Override
    public StageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemStageBinding binding = ItemStageBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new StageViewHolder(binding);
    }
    private Map<Integer, Integer> bestScores = new HashMap<>();

    public void setBestScores(Map<Integer, Integer> bestScores) {
        this.bestScores = bestScores;
        notifyDataSetChanged();
    }
    @Override
    public void onBindViewHolder(@NonNull StageViewHolder holder, int position) {

        holder.binding.nodeContainer.animate().cancel();
        holder.binding.nodeContainer.setScaleX(1f);
        holder.binding.nodeContainer.setScaleY(1f);

        Stage stage = stages.get(position);
        int state = getState(position);
        ItemStageBinding b = holder.binding;

        b.textStageTitle.setText(stage.getTitle());
        Integer best = bestScores.get(position);
        if (best != null) {
            b.textStageSubtitle.setText(b.getRoot().getContext()
                    .getString(R.string.stage_best, stage.getSubtitle(), best));
        } else {
            b.textStageSubtitle.setText(stage.getSubtitle());
        }

        // Reset the circle first, because rows get recycled
        b.textNode.animate().cancel();
        b.textNode.setScaleX(1f);
        b.textNode.setScaleY(1f);

        if (state == STATE_DONE) {
            b.textNode.setBackgroundResource(R.drawable.bg_node_done);
            b.textNode.setText("✓");
        } else if (state == STATE_CURRENT) {
            b.textNode.setBackgroundResource(R.drawable.bg_node_current);
            b.textNode.setText(String.valueOf(position + 1));
            b.textNode.setScaleX(0.8f);
            b.textNode.setScaleY(0.8f);
            b.textNode.animate()
                    .scaleX(1f).scaleY(1f)
                    .setDuration(400)
                    .setInterpolator(new OvershootInterpolator());
        } else {
            b.textNode.setBackgroundResource(R.drawable.bg_node_locked);
            b.textNode.setText(String.valueOf(position + 1));
        }

        // Move the node left / middle / right
        FrameLayout.LayoutParams params =
                (FrameLayout.LayoutParams) b.nodeContainer.getLayoutParams();
        params.gravity = GRAVITY_PATTERN[position % GRAVITY_PATTERN.length];
        b.nodeContainer.setLayoutParams(params);

        // Rope to the next circle (the last stage has nothing to connect to)
        if (position == stages.size() - 1) {
            b.ropeView.setVisibility(View.GONE);
        } else {
            b.ropeView.setVisibility(View.VISIBLE);
            int from = ROPE_PATTERN[position % ROPE_PATTERN.length];
            int to = ROPE_PATTERN[(position + 1) % ROPE_PATTERN.length];
            b.ropeView.setRope(from, to, state == STATE_DONE);
        }

        b.nodeContainer.setOnClickListener(v -> listener.onStageClick(stage, state));
    }

    @Override
    public int getItemCount() {
        return stages.size();
    }

    static class StageViewHolder extends RecyclerView.ViewHolder {
        final ItemStageBinding binding;

        StageViewHolder(ItemStageBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            TouchBounce.attach(binding.nodeContainer);
        }
    }
}