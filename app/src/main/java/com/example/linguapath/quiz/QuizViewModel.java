package com.example.linguapath.quiz;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

// Holds the quiz while the screen is rotated. The Activity is destroyed and rebuilt,
// but this object stays, so the questions, score and chosen answer are not lost.
public class QuizViewModel extends ViewModel {

    private final MutableLiveData<List<QuizQuestion>> questions = new MutableLiveData<>();
    private final MutableLiveData<Boolean> resultReady = new MutableLiveData<>(false);
    private boolean started = false;

    public int index = 0;
    public int score = 0;
    public int selected = -1;        // -1 = not answered yet
    public boolean revealed = false; // listening question: the word is shown on request
    public boolean finished = false;
    public boolean resultSaved = false;
    public boolean confettiShown = false;
    public boolean passed = false;
    public boolean perfectBonus = false;
    public int percent = 0;
    public int xpGained = 0;
    public boolean newBest = false;

    // Which words were answered right and wrong (for the revision list)
    public final List<Integer> correctIds = new ArrayList<>();
    public final List<Integer> wrongIds = new ArrayList<>();

    public LiveData<List<QuizQuestion>> getQuestions() {
        return questions;
    }

    public LiveData<Boolean> getResultReady() {
        return resultReady;
    }

    public void load(Context context, int stageId, String language, boolean revision, String topicId) {
        if (started) {
            return; // already loading, or loaded
        }
        started = true;
        if (topicId != null) {
            QuizRepository.loadTopicQuiz(context, topicId, language, list -> questions.setValue(list));
        } else if (revision) {
            QuizRepository.loadRevisionQuiz(context, language, list -> questions.setValue(list));
        } else {
            QuizRepository.loadQuiz(context, stageId, language, list -> questions.setValue(list));
        }
    }

    // "Try again": new random questions, everything back to zero
    public void restart(Context context, int stageId, String language, boolean revision, String topicId) {
        index = 0;
        score = 0;
        selected = -1;
        revealed = false;
        finished = false;
        resultSaved = false;
        confettiShown = false;
        passed = false;
        perfectBonus = false;
        percent = 0;
        xpGained = 0;
        newBest = false;
        correctIds.clear();
        wrongIds.clear();
        started = false;
        resultReady.setValue(false);
        questions.setValue(null);
        load(context, stageId, language, revision, topicId);
    }

    public void markReady(boolean newBest) {
        this.newBest = newBest;
        resultReady.setValue(true);
    }
}