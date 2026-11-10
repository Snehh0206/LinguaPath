package com.example.linguapath.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {StageEntity.class, WordEntity.class, CachedTranslation.class, WordOfDay.class},
        version = 3, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract ContentDao contentDao();
    public abstract TranslationDao translationDao();

    // One background worker for all database jobs, so the screen never freezes
    public static final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();

    private static volatile AppDatabase instance;
    public abstract WordOfDayDao wordOfDayDao();

    // Version 2 -> 3: adds the word_of_day table and keeps everything else
    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `word_of_day` ("
                    + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "`day` TEXT, `language` TEXT, `english` TEXT, `translated` TEXT, "
                    + "`createdAt` INTEGER NOT NULL)");
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_word_of_day_day_language` "
                    + "ON `word_of_day` (`day`, `language`)");
        }
    };
    // Only one database object for the whole app
    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "linguapath.db")
                            .addMigrations(MIGRATION_2_3)
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return instance;
    }
}