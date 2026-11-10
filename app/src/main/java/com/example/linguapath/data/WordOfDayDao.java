package com.example.linguapath.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface WordOfDayDao {

    @Query("SELECT * FROM word_of_day WHERE day = :day AND language = :language LIMIT 1")
    WordOfDay find(String day, String language);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(WordOfDay word);
}