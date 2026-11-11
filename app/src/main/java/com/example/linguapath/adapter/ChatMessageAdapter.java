package com.example.linguapath.adapter;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.linguapath.R;
import com.example.linguapath.databinding.ItemChatMessageBinding;
import com.example.linguapath.model.ChatMessage;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

// My messages sit on the right (green), other people's on the left (white)
public class ChatMessageAdapter extends RecyclerView.Adapter<ChatMessageAdapter.MessageViewHolder> {

    private final List<ChatMessage> items = new ArrayList<>();
    private final String myUid;
    private final Consumer<ChatMessage> longClickListener;
    private final DateFormat timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT);
    // An invisible character followed by the points: the badge travels inside the message text
    public static final String POINTS_MARK = "\u2063";
    public ChatMessageAdapter(String myUid, Consumer<ChatMessage> longClickListener) {
        this.myUid = myUid;
        this.longClickListener = longClickListener;
    }

    public void add(ChatMessage message) {
        items.add(message);
        notifyItemInserted(items.size() - 1);
    }

    public void clear() {
        items.clear();
        notifyDataSetChanged();
    }

    public void removeByUid(String uid) {
        items.removeIf(m -> m.uid.equals(uid));
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemChatMessageBinding binding = ItemChatMessageBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new MessageViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        ChatMessage m = items.get(position);
        ItemChatMessageBinding b = holder.binding;
        boolean mine = m.uid.equals(myUid);
        int gap = (int) (48 * b.getRoot().getResources().getDisplayMetrics().density);

        // Set everything every time: rows are recycled
        b.textName.setVisibility(mine ? View.GONE : View.VISIBLE);
        b.textName.setText(m.name);
        String shown = m.text;
        int mark = shown.indexOf(POINTS_MARK);
        if (mark >= 0) {
            String pts = shown.substring(mark + 1);
            shown = shown.substring(0, mark);
            if (pts.matches("\\d{1,2}")) {
                b.textPoints.setText(b.getRoot().getResources()
                        .getString(R.string.chat_badge_points, pts));
                b.textPoints.setVisibility(View.VISIBLE);
            } else {
                b.textPoints.setVisibility(View.GONE);
            }
        } else {
            b.textPoints.setVisibility(View.GONE);
        }
        b.textMessage.setText(shown);
        b.textTime.setText(timeFormat.format(new Date(m.ts)));

        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) b.bubble.getLayoutParams();
        params.gravity = mine ? Gravity.END : Gravity.START;
        params.setMargins(mine ? gap : 0, 0, mine ? 0 : gap, 0);
        b.bubble.setLayoutParams(params);
        b.bubble.setBackgroundResource(mine ? R.drawable.bg_card_sage : R.drawable.bg_word_card);

        b.bubble.setOnLongClickListener(v -> {
            longClickListener.accept(m);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        final ItemChatMessageBinding binding;

        MessageViewHolder(ItemChatMessageBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}