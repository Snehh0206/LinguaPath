package com.example.linguapath.quiz;

import com.example.linguapath.model.Word;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.example.linguapath.model.Word;   // already there, keep it
import java.util.HashSet;
import java.util.Set;

// Turns a stage's words into questions. No Android code in here.
public class QuizBuilder {

    private static final int MAX_QUESTIONS = 10;
    private static final int OPTIONS = 4;
    private static final int MIN_BLOCKS = 2; // beginner sentences only: 2 to 4 words
    private static final int MAX_BLOCKS = 4;

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
    // For stage quizzes only: every 4th question becomes "build the sentence" (when the word has an example)
    // For stage quizzes only: the 4th and 8th questions become "build the sentence",
    // using only short beginner sentences (2 to 4 words) from this stage
    public static List<QuizQuestion> buildForStage(List<Word> stageWords, List<Word> pool) {
        List<QuizQuestion> questions = build(stageWords, pool);

        List<Word> easy = new ArrayList<>();
        for (Word w : stageWords) {
            int n = tokens(w.getExample()).size();
            String english = w.getExampleMeaning();
            if (n >= MIN_BLOCKS && n <= MAX_BLOCKS && english != null && !english.isEmpty()) {
                easy.add(w);
            }
        }
        Collections.shuffle(easy);

        int next = 0;
        for (int i = 3; i < questions.size() && next < easy.size(); i += 4) {
            QuizQuestion sentence = makeSentence(easy.get(next++), pool);
            if (sentence != null) {
                questions.set(i, sentence);
            }
        }
        return questions;
    }

    private static List<String> tokens(String sentence) {
        List<String> list = new ArrayList<>();
        if (sentence == null || sentence.trim().isEmpty()) {
            return list;
        }
        for (String t : sentence.trim().split("\\s+")) {
            list.add(t);
        }
        return list;
    }

    // Word blocks = the sentence's words + 2 wrong words taken from other example sentences
    private static QuizQuestion makeSentence(Word word, List<Word> pool) {
        List<String> answer = tokens(word.getExample());
        String english = word.getExampleMeaning();
        if (answer.size() < MIN_BLOCKS || answer.size() > MAX_BLOCKS
                || english == null || english.isEmpty()) {
            return null; // too short, too long, or no example: stays a normal question
        }
        Set<String> used = new HashSet<>(answer);
        List<String> extras = new ArrayList<>();
        List<Word> shuffled = new ArrayList<>(pool);
        Collections.shuffle(shuffled);
        for (Word other : shuffled) {
            if (other.getId() == word.getId()) {
                continue;
            }
            for (String t : tokens(other.getExample())) {
                if (!used.contains(t) && !extras.contains(t)) {
                    extras.add(t);
                    break;
                }
            }
            if (extras.size() == 2) {
                break;
            }
        }
        List<String> blocks = new ArrayList<>(answer);
        blocks.addAll(extras);
        Collections.shuffle(blocks);
        return QuizQuestion.sentence(word.getId(), english, word.getExample(), blocks, answer);
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