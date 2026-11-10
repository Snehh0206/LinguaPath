package com.example.linguapath.quiz;

import com.example.linguapath.model.Word;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Turns a stage's words into questions. No Android code in here.
public class QuizBuilder {

    private static final int MAX_QUESTIONS = 10;
    private static final int OPTIONS = 4;

    // stageWords: the words being tested. pool: all words of the language, for wrong options.
    public static List<QuizQuestion> build(List<Word> stageWords, List<Word> pool) {
        List<Word> words = new ArrayList<>(stageWords);
        Collections.shuffle(words);

        List<QuizQuestion> questions = new ArrayList<>();
        for (int i = 0; i < words.size() && i < MAX_QUESTIONS; i++) {
            // The kinds take turns: meaning, word, listening, meaning, ...
            int kind = i % 3;
            int type = (kind == 0) ? QuizQuestion.TYPE_MEANING
                    : (kind == 1) ? QuizQuestion.TYPE_WORD
                    : QuizQuestion.TYPE_LISTEN;

            QuizQuestion question = make(words.get(i), pool, type);
            if (question != null) {
                questions.add(question);
            }
        }
        return questions;
    }

    private static QuizQuestion make(Word word, List<Word> pool, int type) {
        // Only "which word means this?" has foreign words as options
        boolean optionsAreMeanings = (type != QuizQuestion.TYPE_WORD);
        String correct = optionsAreMeanings ? word.getMeaning() : word.getWord();

        List<Word> shuffled = new ArrayList<>(pool);
        Collections.shuffle(shuffled);

        List<String> options = new ArrayList<>();
        for (Word other : shuffled) {
            if (other.getId() == word.getId()) {
                continue;
            }
            String text = optionsAreMeanings ? other.getMeaning() : other.getWord();
            if (!text.equals(correct) && !options.contains(text)) {
                options.add(text);
            }
            if (options.size() == OPTIONS - 1) {
                break;
            }
        }
        if (options.isEmpty()) {
            return null; // not enough words to make a question
        }
        options.add(correct);
        Collections.shuffle(options);

        String prompt = (type == QuizQuestion.TYPE_WORD) ? word.getMeaning() : word.getWord();
        String sub = (type == QuizQuestion.TYPE_WORD) ? "" : word.getRomanization();
        String speak = (type == QuizQuestion.TYPE_LISTEN) ? word.getWord() : "";

        return new QuizQuestion(word.getId(), type, prompt, sub, speak,
                options, options.indexOf(correct));
    }
}