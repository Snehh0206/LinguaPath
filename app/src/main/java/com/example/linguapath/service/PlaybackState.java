package com.example.linguapath.service;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

// What the screens can read about the playback. The service publishes, screens observe.
public class PlaybackState {

    public static class Snapshot {
        public final boolean active;
        public final boolean playing;
        public final boolean lesson;
        public final String language;
        public final String stageTitle;
        public final int index;       // the word being played (starting at 0)
        public final int total;
        public final String currentWord;
        public final int errorRes;    // 0 = no problem, otherwise a string resource
        public final String errorArg;

        public Snapshot(boolean active, boolean playing, boolean lesson, String language,
                        String stageTitle, int index, int total, String currentWord,
                        int errorRes, String errorArg) {
            this.active = active;
            this.playing = playing;
            this.lesson = lesson;
            this.language = language;
            this.stageTitle = stageTitle;
            this.index = index;
            this.total = total;
            this.currentWord = currentWord;
            this.errorRes = errorRes;
            this.errorArg = errorArg;
        }
    }

    static final Snapshot IDLE = new Snapshot(false, false, false, "", "", 0, 0, "", 0, "");

    private static final MutableLiveData<Snapshot> DATA = new MutableLiveData<>(IDLE);

    public static LiveData<Snapshot> live() {
        return DATA;
    }

    static void publish(Snapshot snapshot) {
        DATA.postValue(snapshot);
    }
}