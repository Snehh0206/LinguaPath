package com.example.linguapath.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

// One row in the "stages" table = one stop on the learning path
@Entity(tableName = "stages")
public class StageEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String language;
    public String title;
    public String level;
    public int orderIndex;

    public StageEntity(String language, String title, String level, int orderIndex) {
        this.language = language;
        this.title = title;
        this.level = level;
        this.orderIndex = orderIndex;
    }
}