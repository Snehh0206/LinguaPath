package com.example.linguapath.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.BounceInterpolator;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.linguapath.R;

import java.util.Random;

// Sprig: a little sprout spirit, drawn with code.
// startIdle() / stopIdle() are called from the Activity's onResume / onPause,
// so no animation or timer ever runs twice or keeps running in the background.
public class SprigView extends View {

    private final Paint bodyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint leafPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint eyePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint blushPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Path leafPath = new Path();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private final float density;

    private float eyeOpen = 1f;     // 1 = open, 0 = closed
    private float happy = 0f;       // 0 = calm, 1 = big smile
    private float leafWiggle = 0f;  // extra leaf angle while wiggling
    private boolean idleRunning = false;

    private ValueAnimator breathAnimator;
    private ValueAnimator blinkAnimator;
    private ValueAnimator wiggleAnimator;

    private final Runnable blinkRunnable = new Runnable() {
        @Override
        public void run() {
            blink();
            handler.postDelayed(this, 2500 + random.nextInt(3000));
        }
    };

    private final Runnable calmRunnable = () -> {
        happy = 0f;
        invalidate();
    };

    public SprigView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        density = context.getResources().getDisplayMetrics().density;

        int ink = ContextCompat.getColor(context, R.color.ink);

        bodyPaint.setStyle(Paint.Style.FILL);
        bodyPaint.setColor(ContextCompat.getColor(context, R.color.sage));

        leafPaint.setStyle(Paint.Style.FILL);
        leafPaint.setColor(ContextCompat.getColor(context, R.color.leaf));

        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeWidth(3 * density);
        linePaint.setColor(ink);

        eyePaint.setStyle(Paint.Style.FILL);
        eyePaint.setColor(ink);

        blushPaint.setStyle(Paint.Style.FILL);
        blushPaint.setColor(ContextCompat.getColor(context, R.color.peach));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // Breathing grows from the bottom, like it is sitting on the ground
        setPivotX(w / 2f);
        setPivotY(h);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        float cx = w / 2f;
        float bodyTop = h * 0.30f;
        float bodyBottom = h * 0.96f;

        // Leaves (behind the body)
        float stemY = bodyTop + h * 0.03f;
        drawLeaf(canvas, cx, stemY, h * 0.30f, -28f - leafWiggle);
        drawLeaf(canvas, cx, stemY, h * 0.26f, 28f + leafWiggle);

        // Body
        rect.set(w * 0.08f, bodyTop, w * 0.92f, bodyBottom);
        canvas.drawOval(rect, bodyPaint);
        canvas.drawOval(rect, linePaint);

        // Eyes
        float eyeY = bodyTop + (bodyBottom - bodyTop) * 0.42f;
        float eyeDx = w * 0.18f;
        float eyeW = w * 0.05f;
        float eyeH = w * 0.065f;

        for (int side = -1; side <= 1; side += 2) {
            float ex = cx + side * eyeDx;
            if (happy >= 0.5f) {
                // Happy eyes: little arches
                rect.set(ex - eyeW * 1.3f, eyeY - eyeH * 0.8f, ex + eyeW * 1.3f, eyeY + eyeH * 0.8f);
                canvas.drawArc(rect, 200f, 140f, false, linePaint);
            } else {
                float halfHeight = Math.max(eyeH * eyeOpen, linePaint.getStrokeWidth() / 2f);
                rect.set(ex - eyeW, eyeY - halfHeight, ex + eyeW, eyeY + halfHeight);
                canvas.drawOval(rect, eyePaint);
            }
        }

        // Blush
        float blushY = eyeY + h * 0.10f;
        for (int side = -1; side <= 1; side += 2) {
            float bx = cx + side * w * 0.27f;
            rect.set(bx - w * 0.06f, blushY - h * 0.03f, bx + w * 0.06f, blushY + h * 0.03f);
            canvas.drawOval(rect, blushPaint);
        }

        // Mouth: a small smile that grows when happy
        float mouthW = w * (0.10f + 0.10f * happy);
        float mouthH = h * (0.05f + 0.08f * happy);
        float mouthY = eyeY + h * 0.07f;
        rect.set(cx - mouthW, mouthY - mouthH, cx + mouthW, mouthY + mouthH);
        canvas.drawArc(rect, 20f, 140f, false, linePaint);
    }

    private void drawLeaf(Canvas canvas, float x, float y, float size, float angle) {
        canvas.save();
        canvas.translate(x, y);
        canvas.rotate(angle);
        leafPath.reset();
        leafPath.moveTo(0, 0);
        leafPath.quadTo(-size * 0.45f, -size * 0.5f, 0, -size);
        leafPath.quadTo(size * 0.45f, -size * 0.5f, 0, 0);
        canvas.drawPath(leafPath, leafPaint);
        canvas.drawPath(leafPath, linePaint);
        canvas.restore();
    }

    // ---- Idle life: breathing and blinking ----

    public void startIdle() {
        if (idleRunning) {
            return; // never start twice
        }
        idleRunning = true;

        breathAnimator = ValueAnimator.ofFloat(0f, 1f);
        breathAnimator.setDuration(1800);
        breathAnimator.setRepeatCount(ValueAnimator.INFINITE);
        breathAnimator.setRepeatMode(ValueAnimator.REVERSE);
        breathAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        breathAnimator.addUpdateListener(a -> {
            float v = (float) a.getAnimatedValue();
            setScaleY(1f + 0.04f * v);
            setScaleX(1f - 0.02f * v);
        });
        breathAnimator.start();

        handler.postDelayed(blinkRunnable, 1500);
    }

    public void stopIdle() {
        idleRunning = false;
        if (breathAnimator != null) {
            breathAnimator.cancel();
            breathAnimator = null;
        }
        if (blinkAnimator != null) {
            blinkAnimator.cancel();
        }
        if (wiggleAnimator != null) {
            wiggleAnimator.cancel();
        }
        handler.removeCallbacks(blinkRunnable);
        handler.removeCallbacks(calmRunnable);
        animate().cancel();
        setTranslationY(0f);
        setScaleX(1f);
        setScaleY(1f);
        eyeOpen = 1f;
        happy = 0f;
        leafWiggle = 0f;
        invalidate();
    }

    private void blink() {
        if (blinkAnimator != null) {
            blinkAnimator.cancel();
        }
        blinkAnimator = ValueAnimator.ofFloat(1f, 0f, 1f);
        blinkAnimator.setDuration(180);
        blinkAnimator.addUpdateListener(a -> {
            eyeOpen = (float) a.getAnimatedValue();
            invalidate();
        });
        blinkAnimator.start();
    }

    // ---- Reaction: jump, smile, wiggle leaves ----

    public void react() {
        animate().cancel();
        handler.removeCallbacks(calmRunnable);
        happy = 1f;
        invalidate();

        animate()
                .translationY(-28f * density)
                .setDuration(180)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(() -> animate()
                        .translationY(0f)
                        .setDuration(500)
                        .setInterpolator(new BounceInterpolator()));

        if (wiggleAnimator != null) {
            wiggleAnimator.cancel();
        }
        wiggleAnimator = ValueAnimator.ofFloat(0f, 18f, -14f, 10f, -6f, 0f);
        wiggleAnimator.setDuration(800);
        wiggleAnimator.addUpdateListener(a -> {
            leafWiggle = (float) a.getAnimatedValue();
            invalidate();
        });
        wiggleAnimator.start();

        handler.postDelayed(calmRunnable, 1400);
    }

    @Override
    protected void onDetachedFromWindow() {
        stopIdle(); // safety: nothing keeps running after the screen is gone
        super.onDetachedFromWindow();
    }
}