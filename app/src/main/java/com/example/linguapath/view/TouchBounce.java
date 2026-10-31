package com.example.linguapath.view;

import android.annotation.SuppressLint;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.OvershootInterpolator;

// Makes any view squish when pressed and spring back when released.
// It returns false, so the normal tap still works.
public class TouchBounce {

    @SuppressLint("ClickableViewAccessibility")
    public static void attach(View view) {
        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL: // CANCEL happens when the list starts scrolling
                    v.animate().scaleX(1f).scaleY(1f).setDuration(300)
                            .setInterpolator(new OvershootInterpolator(3f));
                    break;
            }
            return false;
        });
    }
}