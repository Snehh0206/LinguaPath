package com.example.linguapath.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.annotation.Nullable;

import com.example.linguapath.R;

import java.util.Random;

// A calm, whimsical landscape drawn entirely in code:
// sky, sun, drifting clouds, four layers of hand-wobbled hills, a swaying tree,
// floating pollen and a paper-grain texture.
// Call start() in onResume and stop() in onPause. setScroll() adds parallax depth.
public class SceneryView extends View {

    private static final int HILLS = 4;
    private static final int[] HILL_TOP = {0xFFD6EBE4, 0xFFBFE1D6, 0xFFCDE6D0, 0xFFB5DCB0};
    private static final int[] HILL_BOTTOM = {0xFFC4E1D9, 0xFFA9D4C7, 0xFFB7DBB9, 0xFFA3D19E};
    private static final float[] PARALLAX = {0.08f, 0.16f, 0.26f, 0.38f};
    private static final int NOISE_POINTS = 128;
    private static final int POLLEN_COUNT = 14;

    // Clouds: height (fraction of the sky), size in dp, speed in dp per second, start position
    private static final float[] CLOUD_Y = {0.22f, 0.55f, 0.80f};
    private static final float[] CLOUD_SIZE = {10f, 14f, 8f};
    private static final float[] CLOUD_SPEED = {7f, 5f, 9f};
    private static final float[] CLOUD_X0 = {0.10f, 0.55f, 0.85f};

    private static Bitmap grainBitmap;

    private final float density;
    private final float horizon;
    private final boolean showSun;

    private final Paint skyPaint = newPaint();
    private final Paint sunGlowPaint = newPaint();
    private final Paint sunPaint = newPaint();
    private final Paint rayPaint = newPaint();
    private final Paint cloudPaint = newPaint();
    private final Paint trunkPaint = newPaint();
    private final Paint leafPaint = newPaint();
    private final Paint leafLightPaint = newPaint();
    private final Paint pollenPaint = newPaint();
    private final Paint grainPaint = newPaint();
    private final Paint[] hillPaints = new Paint[HILLS];
    private final Path[] hillPaths = new Path[HILLS];
    private final Path rayPath = new Path();
    private final RectF rect = new RectF();

    private final float[][] noise = new float[HILLS][NOISE_POINTS];
    private final float[] phase = new float[HILLS];
    private final float[] pollenX = new float[POLLEN_COUNT];
    private final float[] pollenY = new float[POLLEN_COUNT];
    private final float[] pollenSpeed = new float[POLLEN_COUNT];
    private final float[] pollenPhase = new float[POLLEN_COUNT];

    private float skyHeight, sunX, sunY, treeX, treeY;
    private float scrollOffset = 0f;
    private long startMs = 0;
    private ValueAnimator ticker;

    public SceneryView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        density = context.getResources().getDisplayMetrics().density;

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SceneryView);
        horizon = a.getFloat(R.styleable.SceneryView_sceneHorizon, 0.55f);
        showSun = a.getBoolean(R.styleable.SceneryView_sceneSun, true);
        a.recycle();

        // A fixed seed: the hills always have the same hand-drawn shape
        Random random = new Random(7);
        for (int i = 0; i < HILLS; i++) {
            phase[i] = random.nextFloat() * 6.28f;
            for (int j = 0; j < NOISE_POINTS; j++) {
                noise[i][j] = random.nextFloat() * 2f - 1f;
            }
            hillPaints[i] = newPaint();
            hillPaths[i] = new Path();
        }
        for (int i = 0; i < POLLEN_COUNT; i++) {
            pollenX[i] = random.nextFloat();
            pollenY[i] = random.nextFloat();
            pollenSpeed[i] = 0.5f + random.nextFloat();
            pollenPhase[i] = random.nextFloat() * 6.28f;
        }

        cloudPaint.setColor(0xEBFFFFFF);
        trunkPaint.setColor(0xFF8A6A55);
        leafPaint.setColor(0xFF4FA593);
        leafLightPaint.setColor(0xFF7CC7B4);
        pollenPaint.setColor(0xFFFFF8E7);
        rayPaint.setColor(0x55F9BE82);
        grainPaint.setShader(new BitmapShader(grain(), Shader.TileMode.REPEAT, Shader.TileMode.REPEAT));

        // One sun ray: a thin triangle pointing outward
        rayPath.moveTo(26 * density, -4 * density);
        rayPath.lineTo(62 * density, 0);
        rayPath.lineTo(26 * density, 4 * density);
        rayPath.close();
    }

    private static Paint newPaint() {
        return new Paint(Paint.ANTI_ALIAS_FLAG);
    }

    // A small tile of faint specks, repeated over the whole scene
    private static Bitmap grain() {
        if (grainBitmap == null) {
            int size = 96;
            int[] pixels = new int[size * size];
            Random random = new Random(11);
            for (int i = 0; i < pixels.length; i++) {
                pixels[i] = Color.argb(random.nextInt(22), 60, 50, 40);
            }
            grainBitmap = Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888);
        }
        return grainBitmap;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        skyHeight = h * horizon;

        skyPaint.setShader(new LinearGradient(0, 0, 0, skyHeight * 1.2f,
                0xFFD2E9F4, 0xFFFDEFD8, Shader.TileMode.CLAMP));

        sunX = w * 0.64f;
        sunY = Math.min(skyHeight * 0.30f, 90 * density);
        sunGlowPaint.setShader(new RadialGradient(sunX, sunY, 80 * density,
                0x66F9BE82, 0x00F9BE82, Shader.TileMode.CLAMP));
        sunPaint.setShader(new RadialGradient(sunX, sunY, 20 * density,
                0xFFFFF1CF, 0xFFF9BE82, Shader.TileMode.CLAMP));

        float extra = 48 * density;
        float step = 8 * density;
        for (int i = 0; i < HILLS; i++) {
            Path p = hillPaths[i];
            p.reset();
            p.moveTo(0, h + extra);
            for (float x = 0; x <= w + step; x += step) {
                float xx = Math.min(x, w);
                p.lineTo(xx, hillY(i, xx, w, h));
            }
            p.lineTo(w, h + extra);
            p.close();

            float top = h * (horizon + i * 0.07f) - hillAmp(i, h) * 1.3f;
            hillPaints[i].setShader(new LinearGradient(0, top, 0, h + extra,
                    HILL_TOP[i], HILL_BOTTOM[i], Shader.TileMode.CLAMP));
        }

        treeX = w * 0.58f;
        treeY = hillY(1, treeX, w, h) + 2 * density;
    }

    private float hillAmp(int index, float h) {
        return Math.min(h * (0.045f + 0.008f * index), 26 * density);
    }

    // The height of hill number "index" at position x: two waves plus a little hand-drawn wobble
    private float hillY(int index, float x, float w, float h) {
        float base = h * (horizon + index * 0.07f);
        float amp = hillAmp(index, h);
        float t = x / w;

        double wave1 = t * (3.2 + index) * Math.PI + phase[index];
        double wave2 = t * (9 + 2 * index) * Math.PI + phase[index] * 2;
        float y = base
                - amp * (0.5f + 0.5f * (float) Math.sin(wave1))
                - amp * 0.35f * (float) Math.sin(wave2);

        float pos = Math.max(0f, Math.min(1f, t)) * (NOISE_POINTS - 1);
        int i = Math.min((int) pos, NOISE_POINTS - 2);
        float f = pos - i;
        f = f * f * (3 - 2 * f); // smooth blend between two random points
        float n = noise[index][i] * (1 - f) + noise[index][i + 1] * f;
        return y - amp * 0.12f * n;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        if (w == 0f) {
            return;
        }
        float time = (startMs == 0) ? 0f : (SystemClock.uptimeMillis() - startMs) / 1000f;

        canvas.drawRect(0, 0, w, h, skyPaint);

        // Far away: sun and two clouds, they barely move when you scroll
        canvas.save();
        canvas.translate(0, -offset(0) * 0.5f);
        if (showSun) {
            drawSun(canvas, time);
        }
        drawCloud(canvas, 0, w, time);
        drawCloud(canvas, 1, w, time);
        canvas.restore();

        drawHill(canvas, 0);

        // A cloud floating in front of the farthest hill
        canvas.save();
        canvas.translate(0, -offset(0));
        drawCloud(canvas, 2, w, time);
        canvas.restore();

        drawHill(canvas, 1);
        drawTree(canvas, time);
        drawHill(canvas, 2);
        drawPollen(canvas, w, h, time);
        drawHill(canvas, 3);

        canvas.drawRect(0, 0, w, h, grainPaint);
    }

    // Layers closer to you move more when the list scrolls. That is the parallax depth.
    private float offset(int layer) {
        return Math.min(scrollOffset * PARALLAX[layer], 28 * density);
    }

    private void drawHill(Canvas canvas, int index) {
        canvas.save();
        canvas.translate(0, -offset(index));
        canvas.drawPath(hillPaths[index], hillPaints[index]);
        canvas.restore();
    }

    private void drawSun(Canvas canvas, float time) {
        canvas.drawCircle(sunX, sunY, 80 * density, sunGlowPaint);

        canvas.save();
        canvas.translate(sunX, sunY);
        canvas.rotate(time * 4f); // slow turning rays
        for (int i = 0; i < 8; i++) {
            canvas.drawPath(rayPath, rayPaint);
            canvas.rotate(45f);
        }
        canvas.restore();

        canvas.drawCircle(sunX, sunY, 20 * density, sunPaint);
    }

    private void drawCloud(Canvas canvas, int index, float w, float time) {
        float s = CLOUD_SIZE[index] * density;
        float span = w + 6 * s;
        float xx = (CLOUD_X0[index] * span + CLOUD_SPEED[index] * density * time) % span;
        float x = xx - 3 * s;
        float y = skyHeight * CLOUD_Y[index]
                + (float) Math.sin(time * 0.5f + index) * 3 * density;

        rect.set(x - 2.2f * s, y - 0.55f * s, x + 2.2f * s, y + 0.55f * s);
        canvas.drawRoundRect(rect, 0.55f * s, 0.55f * s, cloudPaint);
        canvas.drawCircle(x - 0.7f * s, y - 0.55f * s, 0.8f * s, cloudPaint);
        canvas.drawCircle(x + 0.5f * s, y - 0.75f * s, 1.0f * s, cloudPaint);
    }

    private void drawTree(Canvas canvas, float time) {
        float d = density;
        canvas.save();
        canvas.translate(0, -offset(1));
        canvas.drawRoundRect(treeX - 3 * d, treeY - 28 * d, treeX + 3 * d, treeY + 2 * d,
                3 * d, 3 * d, trunkPaint);

        canvas.save();
        canvas.rotate((float) Math.sin(time * 0.9f) * 2.5f, treeX, treeY - 10 * d); // gentle sway
        canvas.drawCircle(treeX - 14 * d, treeY - 30 * d, 13 * d, leafPaint);
        canvas.drawCircle(treeX + 14 * d, treeY - 30 * d, 13 * d, leafPaint);
        canvas.drawCircle(treeX, treeY - 42 * d, 18 * d, leafPaint);
        canvas.drawCircle(treeX - 5 * d, treeY - 47 * d, 9 * d, leafLightPaint);
        canvas.restore();

        canvas.restore();
    }

    private void drawPollen(Canvas canvas, float w, float h, float time) {
        for (int i = 0; i < POLLEN_COUNT; i++) {
            float x = (((pollenX[i] + pollenSpeed[i] * 0.012f * time) % 1f) + 1f) % 1f * w;
            float y = (((pollenY[i] - pollenSpeed[i] * 0.008f * time) % 1f) + 1f) % 1f * h * 0.85f;
            x += (float) Math.sin(time * 1.3f + pollenPhase[i]) * 6 * density;

            float twinkle = 0.5f + 0.5f * (float) Math.sin(time * 2f + pollenPhase[i] * 3f);
            pollenPaint.setAlpha((int) (60 + 150 * twinkle));
            canvas.drawCircle(x, y, (1.4f + 1.4f * twinkle) * density, pollenPaint);
        }
    }

    // ---- Public controls ----

    public void setScroll(float scrollPx) {
        scrollOffset = Math.max(0f, scrollPx);
        invalidate();
    }

    public void start() {
        if (ticker != null) {
            return; // never start twice
        }
        if (startMs == 0) {
            startMs = SystemClock.uptimeMillis();
        }
        // The animator only asks for a redraw every frame. Positions come from the clock.
        ticker = ValueAnimator.ofFloat(0f, 1f);
        ticker.setDuration(1000);
        ticker.setRepeatCount(ValueAnimator.INFINITE);
        ticker.setInterpolator(new LinearInterpolator());
        ticker.addUpdateListener(a -> invalidate());
        ticker.start();
    }

    public void stop() {
        if (ticker != null) {
            ticker.cancel();
            ticker = null;
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        stop();
        super.onDetachedFromWindow();
    }
}