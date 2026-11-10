package com.example.linguapath.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// A theme like Café or Airport, with its words for each language
public class Topic {
    public final String id;
    public final String title;
    public final String emoji;
    public final int theme; // which scene colors to use (see SceneryView)

    private final Map<String, List<TopicWord>> words = new HashMap<>();

    public Topic(String id, String title, String emoji, int theme) {
        this.id = id;
        this.title = title;
        this.emoji = emoji;
        this.theme = theme;
    }

    public void setWords(String language, List<TopicWord> list) {
        words.put(language, list);
    }

    public List<TopicWord> getWords(String language) {
        List<TopicWord> list = words.get(language);
        return list == null ? new ArrayList<>() : list;
    }
}