package com.example.linguapath.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.linguapath.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// A transparent overlay. Put it on top of a screen and call burst() to celebrate.
// It does not block touches.
public class ConfettiView extends View {

    private static final long DURATION_MS = 1800;
    private static final int PARTICLE_COUNT = 70;
    private static final float GRAVITY_DP = 900f;

    private static class Particle {
        float x, y, vx, vy, size, rotation, spin;
        int color;
        boolean sparkle;
    }

    private final List<Particle> particles = new ArrayList<>();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path star = new Path();
    private final Random random = new Random();
    private final float density;
    private final int[] colors;
    private ValueAnimator animator;

    public ConfettiView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        density = context.getResources().getDisplayMetrics().density;
        colors = new int[]{
                ContextCompat.getColor(context, R.color.primary),
                ContextCompat.getColor(context, R.color.accent),
                0xFF8BCFC2,
                0xFFF9BE82,
                0xFF9CC9E6
        };
    }

    // Burst from the upper middle of the screen
    public void burst() {
        burst(getWidth() / 2f, getHeight() * 0.30f);
    }

    public void burst(float originX, float originY) {
        if (getWidth() == 0) {
            return; // not laid out yet
        }
        if (animator != null) {
            animator.cancel();
        }
        particles.clear();
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            Particle p = new Particle();
            p.x = originX;
            p.y = originY;
            // Mostly upward, spread like a fan
            double angle = -Math.PI / 2 + (random.nextDouble() - 0.5) * Math.PI * 1.1;
            float speed = (260 + random.nextFloat() * 520) * density;
            p.vx = (float) Math.cos(angle) * speed;
            p.vy = (float) Math.sin(angle) * speed;
            p.size = (4 + random.nextFloat() * 5) * density;
            p.rotation = random.nextFloat() * 360f;
            p.spin = (random.nextFloat() - 0.5f) * 720f;
            p.color = colors[random.nextInt(colors.length)];
            p.sparkle = random.nextInt(4) == 0;
            particles.add(p);
        }

        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(DURATION_MS);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(a -> invalidate());
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator a) {
                particles.clear();
                invalidate();
            }
        });
        animator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (animator == null || !animator.isRunning()) {
            return;
        }
        float fraction = animator.getAnimatedFraction();
        float t = fraction * DURATION_MS / 1000f; // seconds
        float fade = 1f - Math.max(0f, (fraction - 0.6f) / 0.4f);

        for (Particle p : particles) {
            float x = p.x + p.vx * t;
            float y = p.y + p.vy * t + 0.5f * GRAVITY_DP * density * t * t;

            paint.setColor(p.color);
            paint.setAlpha((int) (255 * fade));

            canvas.save();
            canvas.translate(x, y);
            canvas.rotate(p.rotation + p.spin * t);
            if (p.sparkle) {
                drawStar(canvas, p.size * 1.4f);
            } else {
                canvas.drawRoundRect(-p.size / 2f, -p.size / 4f, p.size / 2f, p.size / 4f,
                        2 * density, 2 * density, paint);
            }
            canvas.restore();
        }
    }

    private void drawStar(Canvas canvas, float s) {
        star.reset();
        star.moveTo(0, -s);
        star.quadTo(0, 0, s, 0);
        star.quadTo(0, 0, 0, s);
        star.quadTo(0, 0, -s, 0);
        star.quadTo(0, 0, 0, -s);
        star.close();
        canvas.drawPath(star, paint);
    }

    @Override
    protected void onDetachedFromWindow() {
        if (animator != null) {
            animator.cancel();
        }
        super.onDetachedFromWindow();
    }
}