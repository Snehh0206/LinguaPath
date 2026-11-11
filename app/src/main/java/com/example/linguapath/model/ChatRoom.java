package com.example.linguapath.model;

import com.example.linguapath.R;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

// The fixed list of chat rooms. To add a room, add one line here.
public class ChatRoom {

    public final String id;      // also the name of the room in the database
    public final int titleRes;
    public final String language;
    public final String emoji;

    public ChatRoom(String id, int titleRes, String language, String emoji) {
        this.id = id;
        this.titleRes = titleRes;
        this.language = language;
        this.emoji = emoji;
    }

    public static final List<ChatRoom> ALL = Collections.unmodifiableList(Arrays.asList(
            new ChatRoom("bn_everyday", R.string.room_bn_everyday, "Bangla", "☕"),
            new ChatRoom("bn_travel", R.string.room_bn_travel, "Bangla", "✈️"),
            new ChatRoom("ja_everyday", R.string.room_ja_everyday, "Japanese", "☕"),
            new ChatRoom("ja_travel", R.string.room_ja_travel, "Japanese", "✈️"),
            new ChatRoom("fr_everyday", R.string.room_fr_everyday, "French", "☕"),
            new ChatRoom("fr_travel", R.string.room_fr_travel, "French", "✈️"),
            new ChatRoom("es_everyday", R.string.room_es_everyday, "Spanish", "☕"),
            new ChatRoom("es_travel", R.string.room_es_travel, "Spanish", "✈️"),
            new ChatRoom("ko_everyday", R.string.room_ko_everyday, "Korean", "☕"),
            new ChatRoom("ko_travel", R.string.room_ko_travel, "Korean", "✈️")));
    // A private chat between two friends (no fixed title, the screen shows the friend's name)
    public static ChatRoom privateRoom(String id, String language) {
        return new ChatRoom(id, 0, language == null ? "Spanish" : language, "💬");
    }
    public static ChatRoom byId(String id) {
        for (ChatRoom room : ALL) {
            if (room.id.equals(id)) {
                return room;
            }
        }
        return null;
    }
}