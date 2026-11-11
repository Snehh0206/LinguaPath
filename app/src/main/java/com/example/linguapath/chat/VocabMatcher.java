package com.example.linguapath.chat;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

// Finds which of the learner's known words appear in a message. No Android code in here.
//   points = 2 for each different known word, up to 3 words per message (so 6 at most)
public class VocabMatcher {

    public static final int POINTS_PER_WORD = 2;
    public static final int MAX_WORDS_PER_MESSAGE = 3;

    public static List<String> findWords(String message, Collection<String> knownWords, String language) {
        // Korean, Japanese and Bangla attach endings or have no spaces, so we look inside the text
        boolean attached = "Korean".equals(language) || "Japanese".equals(language)
                || "Bangla".equals(language);
        int minInside = "Korean".equals(language) ? 2 : "Bangla".equals(language) ? 3 : 1;
        boolean latin = !attached; // accents are removed only for Spanish and French
        String text = normalize(message, latin);
        if (text.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> tokens = java.util.Arrays.asList(text.split(" "));

        Set<String> found = new LinkedHashSet<>();
        for (String known : knownWords) {
            String word = normalize(known, latin);
            if (word.isEmpty()) {
                continue;
            }
            boolean match;
            if (attached) {
                match = (word.length() >= minInside) ? text.contains(word) : startsAToken(tokens, word);
            } else if (word.contains(" ")) {
                match = (" " + text + " ").contains(" " + word + " "); // a phrase
            } else {
                match = tokens.contains(word); // a whole word
            }
            if (match) {
                found.add(word);
            }
        }
        return new ArrayList<>(found);
    }

    public static int pointsFor(List<String> found) {
        return Math.min(found.size(), MAX_WORDS_PER_MESSAGE) * POINTS_PER_WORD;
    }

    private static boolean startsAToken(List<String> tokens, String word) {
        for (String token : tokens) {
            if (token.startsWith(word)) {
                return true;
            }
        }
        return false;
    }

    // Lower case, no punctuation, single spaces. Spanish accents are removed, so "cafe" matches "café".
    private static String normalize(String s, boolean stripAccents) {
        String t = s.toLowerCase(Locale.ROOT);
        if (stripAccents) {
            t = Normalizer.normalize(t, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        }
        return t.replaceAll("[^\\p{L}\\p{M}\\p{N}]+", " ").trim();
    }
}