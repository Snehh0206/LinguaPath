package com.example.linguapath.provider;

import android.net.Uri;

// The public address and column names of the saved-words provider
public final class SavedWordsContract {

    public static final String AUTHORITY = "com.example.linguapath.savedwords";
    public static final String PATH = "saved_words";

    // content://com.example.linguapath.savedwords/saved_words
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + PATH);

    // MIME types: "dir" = a list of rows, "item" = one row
    public static final String CONTENT_TYPE =
            "vnd.android.cursor.dir/vnd.com.example.linguapath.savedword";
    public static final String CONTENT_ITEM_TYPE =
            "vnd.android.cursor.item/vnd.com.example.linguapath.savedword";

    public static final String TABLE = "saved_words";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_USER_ID = "user_id"; // set by the provider, not by callers
    public static final String COLUMN_LANGUAGE = "language";
    public static final String COLUMN_WORD_ID = "word_id";
    public static final String COLUMN_WORD = "word";
    public static final String COLUMN_MEANING = "meaning";
    public static final String COLUMN_EXAMPLE = "example";
    public static final String COLUMN_SAVED_AT = "saved_at";

    private SavedWordsContract() {
    }
}