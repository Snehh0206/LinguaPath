package com.example.linguapath.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {StageEntity.class, WordEntity.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract ContentDao contentDao();

    // One background worker for all database jobs, so the screen never freezes
    public static final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();

    private static volatile AppDatabase instance;

    // Only one database object for the whole app
    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "linguapath.db").build();
                }
            }
        }
        return instance;
    }
}