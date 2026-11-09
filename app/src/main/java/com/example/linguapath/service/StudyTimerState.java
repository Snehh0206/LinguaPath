package com.example.linguapath.service;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

// What every screen can read about the study timer.
// There is no binding: the service publishes, and screens observe.
public class StudyTimerState {

    public static class Snapshot {
        public final boolean active;       // a session exists (running or paused)
        public final boolean running;
        public final boolean manualPaused;
        public final String language;
        public final String stageTitle;
        public final int stageIndex;
        public final long sessionMs;       // time of this stage session
        public final long unsavedMs;       // the part not yet added to the saved totals

        public Snapshot(boolean active, boolean running, boolean manualPaused, String language,
                        String stageTitle, int stageIndex, long sessionMs, long unsavedMs) {
            this.active = active;
            this.running = running;
            this.manualPaused = manualPaused;
            this.language = language;
            this.stageTitle = stageTitle;
            this.stageIndex = stageIndex;
            this.sessionMs = sessionMs;
            this.unsavedMs = unsavedMs;
        }
    }

    static final Snapshot IDLE = new Snapshot(false, false, false, "", "", -1, 0, 0);

    private static final MutableLiveData<Snapshot> DATA = new MutableLiveData<>(IDLE);

    // The stage the user stopped by hand ("language#stageIndex"). It does not restart by itself.
    static String stoppedKey = null;

    public static LiveData<Snapshot> live() {
        return DATA;
    }

    static void publish(Snapshot snapshot) {
        DATA.postValue(snapshot);
    }
}