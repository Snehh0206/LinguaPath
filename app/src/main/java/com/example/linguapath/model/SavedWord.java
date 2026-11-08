package com.example.linguapath.model;

// One saved word, as the screens see it
public class SavedWord {
    private final long id;
    private final int wordId;
    private final String language;
    private final String word;
    private final String meaning;
    private final String example;
    private final long savedAt;

    public SavedWord(long id, int wordId, String language, String word,
                     String meaning, String example, long savedAt) {
        this.id = id;
        this.wordId = wordId;
        this.language = language;
        this.word = word;
        this.meaning = meaning;
        this.example = example;
        this.savedAt = savedAt;
    }

    public long getId() { return id; }
    public int getWordId() { return wordId; }
    public String getLanguage() { return language; }
    public String getWord() { return word; }
    public String getMeaning() { return meaning; }
    public String getExample() { return example; }
    public long getSavedAt() { return savedAt; }
}