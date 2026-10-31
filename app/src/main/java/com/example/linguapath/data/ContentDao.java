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

    @Query("SELECT * FROM stages WHERE language = :language ORDER BY orderIndex")
    List<StageEntity> getStagesForLanguage(String language);

    @Query("SELECT COUNT(*) FROM words WHERE stageId = :stageId")
    int countWordsInStage(int stageId);

    @Query("SELECT * FROM words WHERE stageId = :stageId ORDER BY id")
    List<WordEntity> getWordsForStage(int stageId);

    @Query("SELECT * FROM words WHERE id = :wordId")
    WordEntity getWordById(int wordId);
}