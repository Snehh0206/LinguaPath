package com.example.linguapath.provider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// Private provider for saved words: query, insert and delete.
// It always limits rows to the logged-in user, so accounts never see each other's words.
public class SavedWordsProvider extends ContentProvider {

    private static final int CODE_WORDS = 1; // .../saved_words
    private static final int CODE_WORD = 2;  // .../saved_words/5

    // The matcher turns a Uri into one of the codes above
    private static final UriMatcher MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        MATCHER.addURI(SavedWordsContract.AUTHORITY, SavedWordsContract.PATH, CODE_WORDS);
        MATCHER.addURI(SavedWordsContract.AUTHORITY, SavedWordsContract.PATH + "/#", CODE_WORD);
    }

    private SavedWordsDbHelper helper;

    @Override
    public boolean onCreate() {
        helper = new SavedWordsDbHelper(getContext());
        return true;
    }

    private String currentUserId() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return (user == null) ? "guest" : user.getUid();
    }

    private void notifyChanged() {
        getContext().getContentResolver().notifyChange(SavedWordsContract.CONTENT_URI, null);
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        switch (MATCHER.match(uri)) {
            case CODE_WORDS:
                return SavedWordsContract.CONTENT_TYPE;
            case CODE_WORD:
                return SavedWordsContract.CONTENT_ITEM_TYPE;
            default:
                return null;
        }
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection,
                        @Nullable String selection, @Nullable String[] selectionArgs,
                        @Nullable String sortOrder) {
        int code = MATCHER.match(uri);
        if (code != CODE_WORDS && code != CODE_WORD) {
            throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables(SavedWordsContract.TABLE);
        // Only this user's rows
        builder.appendWhere(SavedWordsContract.COLUMN_USER_ID + " = "
                + DatabaseUtils.sqlEscapeString(currentUserId()));
        if (code == CODE_WORD) {
            builder.appendWhere(" AND " + SavedWordsContract.COLUMN_ID + " = "
                    + ContentUris.parseId(uri));
        }

        String order = TextUtils.isEmpty(sortOrder)
                ? SavedWordsContract.COLUMN_SAVED_AT + " DESC" : sortOrder;

        Cursor cursor = builder.query(helper.getReadableDatabase(),
                projection, selection, selectionArgs, null, null, order);
        // Whoever holds this cursor can be told when the data changes
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        if (MATCHER.match(uri) != CODE_WORDS || values == null) {
            throw new IllegalArgumentException("Cannot insert into: " + uri);
        }
        ContentValues row = new ContentValues(values);
        row.put(SavedWordsContract.COLUMN_USER_ID, currentUserId());
        if (!row.containsKey(SavedWordsContract.COLUMN_SAVED_AT)) {
            row.put(SavedWordsContract.COLUMN_SAVED_AT, System.currentTimeMillis());
        }

        long id = helper.getWritableDatabase().insertWithOnConflict(
                SavedWordsContract.TABLE, null, row, SQLiteDatabase.CONFLICT_IGNORE);
        if (id == -1) {
            return null; // already saved
        }
        notifyChanged();
        return ContentUris.withAppendedId(SavedWordsContract.CONTENT_URI, id);
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection,
                      @Nullable String[] selectionArgs) {
        int code = MATCHER.match(uri);
        String where;
        List<String> args = new ArrayList<>();

        if (code == CODE_WORD) {
            where = SavedWordsContract.COLUMN_ID + " = ?";
            args.add(String.valueOf(ContentUris.parseId(uri)));
        } else if (code == CODE_WORDS) {
            where = TextUtils.isEmpty(selection) ? "1" : "(" + selection + ")";
            if (selectionArgs != null) {
                args.addAll(Arrays.asList(selectionArgs));
            }
        } else {
            throw new IllegalArgumentException("Cannot delete: " + uri);
        }

        // Never delete another user's rows
        where += " AND " + SavedWordsContract.COLUMN_USER_ID + " = ?";
        args.add(currentUserId());

        int rows = helper.getWritableDatabase()
                .delete(SavedWordsContract.TABLE, where, args.toArray(new String[0]));
        if (rows > 0) {
            notifyChanged();
        }
        return rows;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values,
                      @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0; // saved words are never edited, only added or removed
    }
}