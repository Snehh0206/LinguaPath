package com.example.linguapath.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

// DAO = the list of questions we are allowed to ask the database
@Dao
public interface ContentDao {

    @Insert
    long insertStage(StageEntity stage);

    @Insert
    void insertWords(List<WordEntity> words);

    @Query("SELECT COUNT(*) FROM stages")
    int countStages();
    // Every word of one language: the pool for wrong answer options
    @Query("SELECT words.* FROM words INNER JOIN stages ON words.stageId = stages.id WHERE stages.language = :language")
    List<WordEntity> getWordsForLanguage(String language);

    @Query("SELECT * FROM stages WHERE language = :language ORDER BY orderIndex")
    List<StageEntity> getStagesForLanguage(String language);
    @Query("SELECT COUNT(*) FROM stages WHERE language = :language")
    int countStagesForLanguage(String language);
    @Query("SELECT COUNT(*) FROM words WHERE stageId = :stageId")
    int countWordsInStage(int stageId);

    @Query("SELECT * FROM words WHERE stageId = :stageId ORDER BY id")
    List<WordEntity> getWordsForStage(int stageId);

    @Query("SELECT * FROM words WHERE id = :wordId")
    WordEntity getWordById(int wordId);
}