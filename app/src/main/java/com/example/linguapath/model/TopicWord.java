package com.example.linguapath.model;

// One topic word with its picture (an emoji)
public class TopicWord {
    public final String word;
    public final String romanization;
    public final String meaning;
    public final String emoji;

    public TopicWord(String word, String romanization, String meaning, String emoji) {
        this.word = word;
        this.romanization = romanization;
        this.meaning = meaning;
        this.emoji = emoji;
    }
}