package com.example.linguapath.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

// One row in the "words" table = one vocabulary word that belongs to a stage
@Entity(tableName = "words")
public class WordEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public int stageId;
    public String word;
    public String romanization;
    public String meaning;
    public String example;
    public String exampleMeaning;

    public WordEntity(int stageId, String word, String romanization,
                      String meaning, String example, String exampleMeaning) {
        this.stageId = stageId;
        this.word = word;
        this.romanization = romanization;
        this.meaning = meaning;
        this.example = example;
        this.exampleMeaning = exampleMeaning;
    }
}