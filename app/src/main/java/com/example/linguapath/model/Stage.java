package com.example.linguapath.model;

// One stop on the learning path, as the screens see it
public class Stage {
    private final int id;          // database id
    private final int orderIndex;  // position on the path: 0, 1, 2...
    private final String title;
    private final String subtitle;

    public Stage(int id, int orderIndex, String title, String subtitle) {
        this.id = id;
        this.orderIndex = orderIndex;
        this.title = title;
        this.subtitle = subtitle;
    }

    public int getId() { return id; }
    public int getOrderIndex() { return orderIndex; }
    public String getTitle() { return title; }
    public String getSubtitle() { return subtitle; }
}