package com.example.linguapath.view;

import android.annotation.SuppressLint;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

// Gives any view a 3D press: it sinks a little and tilts toward the finger,
// then springs back. It returns false, so the normal tap still works.
public class TouchBounce {

    private static final float MAX_TILT_DEGREES = 8f;

    @SuppressLint("ClickableViewAccessibility")
    public static void attach(View view) {
        float density = view.getResources().getDisplayMetrics().density;
        view.setCameraDistance(density * 8000f); // a softer perspective

        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate().cancel();
                    v.animate().scaleX(0.97f).scaleY(0.97f).translationZ(-3 * density)
                            .setDuration(80).setInterpolator(new DecelerateInterpolator());
                    tilt(v, event);
                    break;
                case MotionEvent.ACTION_MOVE:
                    tilt(v, event);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL: // CANCEL happens when a list starts scrolling
                    v.animate().scaleX(1f).scaleY(1f).translationZ(0f)
                            .rotationX(0f).rotationY(0f)
                            .setDuration(350).setInterpolator(new OvershootInterpolator(2f));
                    break;
            }
            return false;
        });
    }

    // px and py go from -0.5 (left / top) to +0.5 (right / bottom).
    // If your cards tilt toward the finger instead of away, flip the two minus signs.
    private static void tilt(View v, MotionEvent event) {
        float px = clamp(event.getX() / Math.max(1, v.getWidth())) - 0.5f;
        float py = clamp(event.getY() / Math.max(1, v.getHeight())) - 0.5f;
        v.setRotationY(-px * 2f * MAX_TILT_DEGREES);
        v.setRotationX(py * 2f * MAX_TILT_DEGREES);
    }

    private static float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }
}