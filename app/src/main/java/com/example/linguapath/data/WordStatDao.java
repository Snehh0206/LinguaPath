package com.example.linguapath.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface WordStatDao {

    @Query("SELECT * FROM word_stats WHERE userId = :userId AND wordId = :wordId LIMIT 1")
    WordStat find(String userId, int wordId);

    // Only words that were missed at least once can need revision
    @Query("SELECT * FROM word_stats WHERE userId = :userId AND language = :language AND wrongCount > 0")
    List<WordStat> candidates(String userId, String language);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(WordStat stat);
}