package com.example.linguapath.model;

import com.example.linguapath.data.QuizAttempt;

import java.util.ArrayList;
import java.util.List;

// All the numbers for the Progress screen, gathered in one place
public class ProgressSnapshot {

    public static class LanguageProgress {
        public final String language;
        public final int done;
        public final int total;
        public int revisionCount;
        public String revisionPreview = "";

        public LanguageProgress(String language, int done, int total) {
            this.language = language;
            this.done = done;
            this.total = total;
        }
    }

    public static class Achievement {
        public final String title;
        public final String description;
        public final boolean unlocked;

        public Achievement(String title, String description, boolean unlocked) {
            this.title = title;
            this.description = description;
            this.unlocked = unlocked;
        }
    }

    public int xp;
    public int streak;
    public int studyMinutesToday;
    public int savedWords;
    public int quizCount;
    public int perfectCount;
    public final List<LanguageProgress> languages = new ArrayList<>();
    public final List<Achievement> achievements = new ArrayList<>();
    public List<QuizAttempt> attempts = new ArrayList<>();
}