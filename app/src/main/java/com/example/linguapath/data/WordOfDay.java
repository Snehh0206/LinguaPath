package com.example.linguapath.data;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

// The word of one day, in one language
@Entity(tableName = "word_of_day",
        indices = {@Index(value = {"day", "language"}, unique = true)})
public class WordOfDay {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String day;        // for example 2026-11-09
    public String language;
    public String english;
    public String translated;
    public long createdAt;

    public WordOfDay(String day, String language, String english, String translated, long createdAt) {
        this.day = day;
        this.language = language;
        this.english = english;
        this.translated = translated;
        this.createdAt = createdAt;
    }
}