package com.example.linguapath.data;

import com.example.linguapath.model.Stage;

import java.util.ArrayList;
import java.util.List;

// Gives the list of stages for a language.
// Placeholder for now: in Milestone 4 this will read from the database.
public class StageRepository {

    public static List<Stage> getStages(String language) {
        List<Stage> stages = new ArrayList<>();
        if (language.equals("Korean")) {
            stages.add(new Stage(0, "Hangul Basics", "Level 1"));
            stages.add(new Stage(1, "Greetings", "Level 1"));
            stages.add(new Stage(2, "Numbers", "Level 1"));
            stages.add(new Stage(3, "Food", "Level 1"));
            stages.add(new Stage(4, "Travel", "Level 2"));
        } else {
            stages.add(new Stage(0, "Greetings", "A1"));
            stages.add(new Stage(1, "Food & Drink", "A1"));
            stages.add(new Stage(2, "Travel", "A1"));
            stages.add(new Stage(3, "Numbers", "A1"));
            stages.add(new Stage(4, "Daily Life", "A2"));
        }
        return stages;
    }
}