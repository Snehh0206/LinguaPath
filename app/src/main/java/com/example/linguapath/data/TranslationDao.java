package com.example.linguapath.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface TranslationDao {

    @Query("SELECT * FROM cached_translations WHERE langPair = :langPair AND sourceKey = :sourceKey LIMIT 1")
    CachedTranslation find(String langPair, String sourceKey);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(CachedTranslation item);
}