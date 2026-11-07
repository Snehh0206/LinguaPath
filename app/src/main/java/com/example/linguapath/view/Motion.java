package com.example.linguapath.view;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ProgressBar;

import java.util.function.IntConsumer;

// Small reusable animations. Every screen uses these instead of writing its own.
public class Motion {

    // Fade in while sliding up 24dp
    public static void fadeSlideIn(View view, long delayMs) {
        float distance = 24 * view.getResources().getDisplayMetrics().density;
        view.setAlpha(0f);
        view.setTranslationY(distance);
        view.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(delayMs)
                .setDuration(450)
                .setInterpolator(new DecelerateInterpolator(2f))
                // Clear the delay so later animations on this view are not delayed
                .withEndAction(() -> view.animate().setStartDelay(0));
    }

    // Counts a number up (or down) and tells you each value
    public static ValueAnimator countUp(int from, int to, long durationMs, IntConsumer onValue) {
        ValueAnimator animator = ValueAnimator.ofInt(from, to);
        animator.setDuration(durationMs);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(a -> onValue.accept((int) a.getAnimatedValue()));
        animator.start();
        return animator;
    }

    // The bar works in steps of 100 so the movement looks smooth
    public static void animateProgress(ProgressBar bar, int completed, int total) {
        bar.setMax(Math.max(1, total) * 100);
        ObjectAnimator animator = ObjectAnimator.ofInt(bar, "progress", bar.getProgress(), completed * 100);
        animator.setDuration(700);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.start();
    }

    // A gentle up and down loop. Keep the result and pass it to stopFloat in onPause.
    public static ObjectAnimator startFloat(View view, float amplitudeDp, long startDelayMs) {
        float amplitude = amplitudeDp * view.getResources().getDisplayMetrics().density;
        ObjectAnimator animator = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, 0f, -amplitude);
        animator.setDuration(2200);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setRepeatMode(ValueAnimator.REVERSE);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.setStartDelay(startDelayMs);
        animator.start();
        return animator;
    }

    public static void stopFloat(ObjectAnimator animator, View view) {
        if (animator != null) {
            animator.cancel();
        }
        view.setTranslationY(0f);
    }
}