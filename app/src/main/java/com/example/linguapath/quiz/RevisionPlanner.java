package com.example.linguapath.quiz;

import com.example.linguapath.data.WordStat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Decides which words to revise. No Android code in here, so it is easy to explain and test.
//
//   score = wrong * 3 - correct * 2 + days since last seen (at most 7)
//
// - Mistakes count the most.
// - Correct answers cancel mistakes, but more slowly.
// - Time passing brings a word back, so you see it again after a few days.
// A word is suggested when its score is above 0 (and it was missed at least once).
public class RevisionPlanner {

    public static final int MAX_WORDS = 10;
    private static final long DAY_MS = 24L * 60 * 60 * 1000;

    public static int score(WordStat stat, long now) {
        long days = Math.max(0, (now - stat.lastSeen) / DAY_MS);
        return stat.wrongCount * 3 - stat.correctCount * 2 + (int) Math.min(days, 7);
    }

    // The words that need revision, the most urgent first
    public static List<WordStat> pick(List<WordStat> stats, long now) {
        List<WordStat> due = new ArrayList<>();
        for (WordStat stat : stats) {
            if (stat.wrongCount > 0 && score(stat, now) > 0) {
                due.add(stat);
            }
        }
        Collections.sort(due, (a, b) -> Integer.compare(score(b, now), score(a, now)));
        return due.size() > MAX_WORDS ? new ArrayList<>(due.subList(0, MAX_WORDS)) : due;
    }
}