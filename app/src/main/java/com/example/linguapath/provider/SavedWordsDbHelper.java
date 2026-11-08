package com.example.linguapath.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

// Creates the small SQLite database file for saved words
public class SavedWordsDbHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "saved_words.db";
    private static final int DB_VERSION = 1;

    public SavedWordsDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // UNIQUE: the same user can't save the same word twice
        db.execSQL("CREATE TABLE " + SavedWordsContract.TABLE + " ("
                + SavedWordsContract.COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + SavedWordsContract.COLUMN_USER_ID + " TEXT NOT NULL, "
                + SavedWordsContract.COLUMN_LANGUAGE + " TEXT NOT NULL, "
                + SavedWordsContract.COLUMN_WORD_ID + " INTEGER, "
                + SavedWordsContract.COLUMN_WORD + " TEXT NOT NULL, "
                + SavedWordsContract.COLUMN_MEANING + " TEXT, "
                + SavedWordsContract.COLUMN_EXAMPLE + " TEXT, "
                + SavedWordsContract.COLUMN_SAVED_AT + " INTEGER NOT NULL, "
                + "UNIQUE(" + SavedWordsContract.COLUMN_USER_ID + ", "
                + SavedWordsContract.COLUMN_LANGUAGE + ", "
                + SavedWordsContract.COLUMN_WORD + "))");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Version 1 only. Later versions would go here.
    }
}