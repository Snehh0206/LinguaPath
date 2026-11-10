package com.example.linguapath.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface QuizAttemptDao {

    @Insert
    void insert(QuizAttempt attempt);

    @Query("SELECT MAX(percent) FROM quiz_attempts WHERE userId = :userId AND language = :language AND stageIndex = :stageIndex")
    Integer bestPercent(String userId, String language, int stageIndex);
    @Query("SELECT * FROM quiz_attempts WHERE userId = :userId ORDER BY createdAt DESC LIMIT :limit")
    List<QuizAttempt> latest(String userId, int limit);

    @Query("SELECT COUNT(*) FROM quiz_attempts WHERE userId = :userId")
    int count(String userId);

    @Query("SELECT COUNT(*) FROM quiz_attempts WHERE userId = :userId AND percent = 100 AND redo = 0")
    int perfectCount(String userId);

    // The days (like 2026-11-03) on which this user finished a quiz
    @Query("SELECT DISTINCT strftime('%Y-%m-%d', createdAt / 1000, 'unixepoch', 'localtime') FROM quiz_attempts WHERE userId = :userId")
    List<String> attemptDays(String userId);
    @Query("SELECT stageIndex, MAX(percent) AS best FROM quiz_attempts WHERE userId = :userId AND language = :language GROUP BY stageIndex")
    List<BestScore> bestScores(String userId, String language);
}