package com.example.linguapath.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.linguapath.R;
import com.example.linguapath.databinding.ItemChatRoomBinding;
import com.example.linguapath.model.ChatRoom;
import com.example.linguapath.view.TouchBounce;

import java.util.ArrayList;
import java.util.List;

public class ChatRoomAdapter extends RecyclerView.Adapter<ChatRoomAdapter.RoomViewHolder> {

    public interface Listener {
        void onRoom(ChatRoom room);
    }

    private final List<ChatRoom> rooms = new ArrayList<>();
    private final Listener listener;

    public ChatRoomAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setRooms(List<ChatRoom> newRooms) {
        rooms.clear();
        rooms.addAll(newRooms);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RoomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemChatRoomBinding binding = ItemChatRoomBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new RoomViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull RoomViewHolder holder, int position) {
        ChatRoom room = rooms.get(position);
        ItemChatRoomBinding b = holder.binding;
        android.content.Context context = b.getRoot().getContext();

        holder.itemView.animate().cancel();
        holder.itemView.setScaleX(1f);
        holder.itemView.setScaleY(1f);

        b.textRoomEmoji.setText(room.emoji);
        b.textRoomName.setText(room.titleRes);
        b.textRoomSub.setText(context.getString(R.string.room_subtitle, room.language));
        b.getRoot().setOnClickListener(v -> listener.onRoom(room));
    }

    @Override
    public int getItemCount() {
        return rooms.size();
    }

    static class RoomViewHolder extends RecyclerView.ViewHolder {
        final ItemChatRoomBinding binding;

        RoomViewHolder(ItemChatRoomBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            TouchBounce.attach(binding.getRoot());
        }
    }
}