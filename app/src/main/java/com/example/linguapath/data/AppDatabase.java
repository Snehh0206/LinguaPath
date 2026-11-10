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
import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {StageEntity.class, WordEntity.class, CachedTranslation.class, QuizAttempt.class, WordOfDay.class, WordStat.class},
        version = 4, exportSchema = false)
//public abstract class AppDatabase extends RoomDatabase {
public abstract class AppDatabase extends RoomDatabase {

    public abstract ContentDao contentDao();
    public abstract TranslationDao translationDao();
    public abstract QuizAttemptDao quizAttemptDao();

    // One background worker for all database jobs, so the screen never freezes
    public static final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();

    private static volatile AppDatabase instance;
    //public abstract QuizAttemptDao quizAttemptDao();
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
    public abstract WordStatDao wordStatDao();

    // Version 3 -> 4: adds the word_stats table and keeps everything else
    static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `word_stats` ("
                    + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "`userId` TEXT, "
                    + "`language` TEXT, "
                    + "`wordId` INTEGER NOT NULL, "
                    + "`correctCount` INTEGER NOT NULL, "
                    + "`wrongCount` INTEGER NOT NULL, "
                    + "`lastSeen` INTEGER NOT NULL)");
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_word_stats_userId_wordId` "
                    + "ON `word_stats` (`userId`, `wordId`)");
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
                            .addMigrations(MIGRATION_3_4)
                            .fallbackToDestructiveMigration() // only for very old installs
                            .build();
                }
            }
        }
        return instance;
    }
}