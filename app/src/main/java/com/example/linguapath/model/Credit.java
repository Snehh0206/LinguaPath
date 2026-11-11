package com.example.linguapath.model;

// One line on the Credits screen. url is null when there is no website.
public class Credit {
    public final String title;
    public final String body;
    public final String url;

    public Credit(String title, String body, String url) {
        this.title = title;
        this.body = body;
        this.url = url;
    }
}