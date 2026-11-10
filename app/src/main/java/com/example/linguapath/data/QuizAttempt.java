package com.example.linguapath.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

// One finished quiz. The Progress screen will list these.
@Entity(tableName = "quiz_attempts")
public class QuizAttempt {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String userId;
    public String language;
    public int stageIndex;
    public String stageTitle;
    public int correct;
    public int total;
    public int percent;
    public boolean passed;
    public boolean redo;
    public long createdAt;

    public QuizAttempt(String userId, String language, int stageIndex, String stageTitle,
                       int correct, int total, int percent, boolean passed,
                       boolean redo, long createdAt) {
        this.userId = userId;
        this.language = language;
        this.stageIndex = stageIndex;
        this.stageTitle = stageTitle;
        this.correct = correct;
        this.total = total;
        this.percent = percent;
        this.passed = passed;
        this.redo = redo;
        this.createdAt = createdAt;
    }
}