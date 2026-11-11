package com.example.linguapath.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.linguapath.R;
import com.example.linguapath.chat.ChatLevels;
import com.example.linguapath.data.ChatScoreRepository;
import com.example.linguapath.databinding.ItemLeaderBinding;

import java.util.ArrayList;
import java.util.List;

public class LeaderAdapter extends RecyclerView.Adapter<LeaderAdapter.LeaderViewHolder> {

    private static final String[] MEDALS = {"🥇", "🥈", "🥉"};

    private final List<ChatScoreRepository.Entry> items = new ArrayList<>();
    private final String myUid;

    public LeaderAdapter(String myUid) {
        this.myUid = myUid;
    }
    private boolean xpMode = false;

    public void setMode(boolean xp) {
        xpMode = xp;
    }

    public void setItems(List<ChatScoreRepository.Entry> entries) {
        items.clear();
        items.addAll(entries);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public LeaderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemLeaderBinding binding = ItemLeaderBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new LeaderViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull LeaderViewHolder holder, int position) {
        ChatScoreRepository.Entry e = items.get(position);
        ItemLeaderBinding b = holder.binding;
        android.content.Context context = b.getRoot().getContext();
        boolean mine = e.uid.equals(myUid);

        b.textRank.setText(position < 3 ? MEDALS[position] : String.valueOf(position + 1));
        b.textLeaderName.setText(mine ? context.getString(R.string.leaderboard_you, e.name) : e.name);
        if (xpMode) {
            b.textLeaderLevel.setText(context.getString(R.string.account_level,
                    com.example.linguapath.data.RewardRepository.levelForXp(e.points)));
            b.textLeaderPoints.setText(context.getString(R.string.leaderboard_xp, e.points));
        } else {
            b.textLeaderLevel.setText(ChatLevels.nameFor(e.points));
            b.textLeaderPoints.setText(context.getString(R.string.leaderboard_points, e.points));
        }
        b.getRoot().setBackgroundResource(mine ? R.drawable.bg_card_peach : R.drawable.bg_word_card);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class LeaderViewHolder extends RecyclerView.ViewHolder {
        final ItemLeaderBinding binding;

        LeaderViewHolder(ItemLeaderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}