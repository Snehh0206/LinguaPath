package com.example.linguapath.quiz;

import java.util.List;

// One quiz question with its answer options
public class QuizQuestion {

    public static final int TYPE_MEANING = 0; // a foreign word is shown, choose its meaning
    public static final int TYPE_WORD = 1;    // a meaning is shown, choose the foreign word
    public static final int TYPE_LISTEN = 2;  // the word is spoken, choose its meaning

    public final int wordId;
    public final int type;
    public final String prompt;
    public final String promptSub;   // romanization, or ""
    public final String speakText;   // what to speak for listening questions, or ""
    public final List<String> options;
    public final int correctIndex;

    public QuizQuestion(int wordId, int type, String prompt, String promptSub,
                        String speakText, List<String> options, int correctIndex) {
        this.wordId = wordId;
        this.type = type;
        this.prompt = prompt;
        this.promptSub = promptSub;
        this.speakText = speakText;
        this.options = options;
        this.correctIndex = correctIndex;
    }
}