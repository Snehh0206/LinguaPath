package com.example.linguapath.model;

public class ChatMessage {
    public final String id;
    public final String uid;
    public final String name;
    public final String text;
    public final long ts;

    public ChatMessage(String id, String uid, String name, String text, long ts) {
        this.id = id;
        this.uid = uid;
        this.name = name;
        this.text = text;
        this.ts = ts;
    }
}