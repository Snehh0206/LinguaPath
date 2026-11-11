package com.example.linguapath.util;

import android.graphics.Typeface;
import android.icu.text.Transliterator;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;

import java.util.regex.Pattern;

// Writes Korean, Bangla and Japanese (kana) text in Latin letters, using Android's built-in ICU.
// Call on the main thread only.
public class Romanizer {

    private static Transliterator toLatin;
    private static final Pattern HAN = Pattern.compile("\\p{IsHan}");

    public static synchronized String of(String language, String text) {
        if (text == null) {
            return "";
        }
        String t = text.trim();
        boolean supported = "Korean".equals(language) || "Bangla".equals(language)
                || "Japanese".equals(language);
        if (!supported || t.isEmpty() || t.length() > 300) {
            return "";
        }
        if ("Japanese".equals(language) && HAN.matcher(t).find()) {
            return ""; // kanji readings can't be worked out reliably
        }
        try {
            if (toLatin == null) {
                toLatin = Transliterator.getInstance("Any-Latin; Latin-ASCII");
            }
            String out = toLatin.transliterate(t).trim();
            return out.equalsIgnoreCase(t) ? "" : out;
        } catch (Exception e) {
            return "";
        }
    }

    // The text, then its romanization on a second line (smaller, italic). Plain text if none.
    public static CharSequence withRoman(String language, String text) {
        String roman = of(language, text);
        if (roman.isEmpty()) {
            return text; // never an empty romanization line
        }
        SpannableStringBuilder sb = new SpannableStringBuilder(text);
        sb.append("\n");
        int start = sb.length();
        sb.append(roman);
        sb.setSpan(new RelativeSizeSpan(0.7f), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.setSpan(new StyleSpan(Typeface.ITALIC), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return sb;
    }
}