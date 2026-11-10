package com.example.linguapath;

import android.app.Application;

import com.example.linguapath.system.BannerController;

public class LinguaPathApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // The banner controller follows every screen's lifecycle
        registerActivityLifecycleCallbacks(new BannerController(this));
    }
}