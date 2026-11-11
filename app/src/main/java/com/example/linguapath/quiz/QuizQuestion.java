package com.example.linguapath.quiz;

import java.util.Collections;
import java.util.List;

// One quiz question with its answer options
public class QuizQuestion {

    public static final int TYPE_MEANING = 0; // a foreign word is shown, choose its meaning
    public static final int TYPE_WORD = 1;    // a meaning is shown, choose the foreign word
    public static final int TYPE_LISTEN = 2;  // the word is spoken, choose its meaning
    public static final int TYPE_BUILD = 3;   // an English sentence is shown, build it from word blocks

    public final int wordId;
    public final int type;
    public final String prompt;
    public final String promptSub;   // romanization, or ""
    public final String speakText;   // what to speak for listening questions, or the full sentence for build questions
    public final List<String> options; // answer options, or the word blocks for build questions
    public final int correctIndex;     // -1 for build questions
    public final List<String> answer;  // build questions: the blocks in the right order

    public QuizQuestion(int wordId, int type, String prompt, String promptSub,
                        String speakText, List<String> options, int correctIndex) {
        this(wordId, type, prompt, promptSub, speakText, options, correctIndex, Collections.emptyList());
    }

    private QuizQuestion(int wordId, int type, String prompt, String promptSub,
                         String speakText, List<String> options, int correctIndex, List<String> answer) {
        this.wordId = wordId;
        this.type = type;
        this.prompt = prompt;
        this.promptSub = promptSub;
        this.speakText = speakText;
        this.options = options;
        this.correctIndex = correctIndex;
        this.answer = answer;
    }

    // A "build the sentence" question
    public static QuizQuestion sentence(int wordId, String englishSentence, String foreignSentence,
                                        List<String> blocks, List<String> answer) {
        return new QuizQuestion(wordId, TYPE_BUILD, englishSentence, "", foreignSentence,
                blocks, -1, answer);
    }
}