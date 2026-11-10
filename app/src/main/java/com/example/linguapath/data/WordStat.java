package com.example.linguapath.data;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

// How well one user knows one word: how often right, how often wrong, and when last seen
@Entity(tableName = "word_stats",
        indices = {@Index(value = {"userId", "wordId"}, unique = true)})
public class WordStat {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String userId;
    public String language;
    public int wordId;
    public int correctCount;
    public int wrongCount;
    public long lastSeen;

    public WordStat(String userId, String language, int wordId,
                    int correctCount, int wrongCount, long lastSeen) {
        this.userId = userId;
        this.language = language;
        this.wordId = wordId;
        this.correctCount = correctCount;
        this.wrongCount = wrongCount;
        this.lastSeen = lastSeen;
    }
}