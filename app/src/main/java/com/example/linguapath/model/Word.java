package com.example.linguapath.model;

// One vocabulary word, as the screens see it
public class Word {
    private final int id;
    private final String word;
    private final String romanization;
    private final String meaning;
    private final String example;
    private final String exampleMeaning;

    public Word(int id, String word, String romanization,
                String meaning, String example, String exampleMeaning) {
        this.id = id;
        this.word = word;
        this.romanization = romanization;
        this.meaning = meaning;
        this.example = example;
        this.exampleMeaning = exampleMeaning;
    }

    public int getId() { return id; }
    public String getWord() { return word; }
    public String getRomanization() { return romanization; }
    public String getMeaning() { return meaning; }
    public String getExample() { return example; }
    public String getExampleMeaning() { return exampleMeaning; }
}