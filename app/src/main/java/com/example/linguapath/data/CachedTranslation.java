package com.example.linguapath.data;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

// One translation we already got from the API. Asking again costs nothing.
@Entity(tableName = "cached_translations",
        indices = {@Index(value = {"langPair", "sourceKey"}, unique = true)})
public class CachedTranslation {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String langPair;        // for example "en|ko"
    public String sourceKey;       // the English text, trimmed and in lower case
    public String translatedText;
    public long createdAt;

    public CachedTranslation(String langPair, String sourceKey,
                             String translatedText, long createdAt) {
        this.langPair = langPair;
        this.sourceKey = sourceKey;
        this.translatedText = translatedText;
        this.createdAt = createdAt;
    }
}