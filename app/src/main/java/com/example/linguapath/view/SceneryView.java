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

// A calm, whimsical landscape drawn entirely in code.
// sceneTheme changes the colors: 0 meadow, 1 café, 2 airport, 3 shop, 4 festival, 5 travel, 6 business.
// Call start() in onResume and stop() in onPause. setScroll() adds parallax depth.
public class SceneryView extends View {

    private static final int HILLS = 4;
    private static final int NOISE_POINTS = 128;
    private static final int POLLEN_COUNT = 14;
    private static final int CREAM = 0xFFF7F5F0;
    private static final float[] PARALLAX = {0.08f, 0.16f, 0.26f, 0.38f};

    private static final int[] SKY_TOP = {
            0xFFD2E9F4, 0xFFF3DEC6, 0xFFBFE0F5, 0xFFFBE0E6, 0xFF4A4A7D, 0xFFCDE9F1, 0xFFD5DEE8};
    private static final int[] SKY_BOTTOM = {
            0xFFFDEFD8, 0xFFFDF0DF, 0xFFE8F4FB, 0xFFFFEFD9, 0xFFF4A68B, 0xFFFDE9C8, 0xFFEDEFF2};
    private static final int[][] HILL_BASE = {
            {0xFFD6EBE4, 0xFFBFE1D6, 0xFFCDE6D0, 0xFFEEF5E8},
            {0xFFE8D2B5, 0xFFD9BC98, 0xFFEAD9BE, 0xFFF4EBDD},
            {0xFFD5E4EE, 0xFFC2D6E6, 0xFFD2E3EA, 0xFFE9F0F4},
            {0xFFF6D5DA, 0xFFEFC3CB, 0xFFF4D9D3, 0xFFFAEBE6},
            {0xFF5E5A8C, 0xFF6F6A9C, 0xFF8A7BA6, 0xFFD9C3C7},
            {0xFFBFE3D9, 0xFFA9D8C9, 0xFFC9E5C4, 0xFFEAF3DF},
            {0xFFCBD7E1, 0xFFB9C8D5, 0xFFD3DDE5, 0xFFECF0F3}};

    // Clouds: height (fraction of the sky), size in dp, speed in dp per second, start position
    private static final float[] CLOUD_Y = {0.22f, 0.55f, 0.80f};
    private static final float[] CLOUD_SIZE = {10f, 14f, 8f};
    private static final float[] CLOUD_SPEED = {7f, 5f, 9f};
    private static final float[] CLOUD_X0 = {0.10f, 0.55f, 0.85f};

    private static final int[] BALLOON_COLORS = {0xFFF29CB0, 0xFFF9BE82, 0xFF9CC9E6, 0xFFB7DBB9};

    private static Bitmap grainBitmap;

    private final float density;
    private final boolean showSun;
    private int theme;
    private final float horizon;

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
    private final Paint accentPaint = newPaint();
    private final Paint strokePaint = newPaint();
    private final Path tmpPath = new Path();
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
        theme = clampTheme(a.getInt(R.styleable.SceneryView_sceneTheme, 0));
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

        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeCap(Paint.Cap.ROUND);
    }

    private static Paint newPaint() {
        return new Paint(Paint.ANTI_ALIAS_FLAG);
    }

    private static int clampTheme(int value) {
        return Math.max(0, Math.min(value, SKY_TOP.length - 1));
    }

    private static int shade(int color, float factor) {
        int r = (int) (((color >> 16) & 0xFF) * factor);
        int g = (int) (((color >> 8) & 0xFF) * factor);
        int b = (int) ((color & 0xFF) * factor);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
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

    public void setSceneTheme(int newTheme) {
        theme = clampTheme(newTheme);
        if (getWidth() > 0) {
            rebuild(getWidth(), getHeight());
        }
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        rebuild(w, h);
    }

    // Builds everything that depends on the size and the theme
    private void rebuild(int w, int h) {
        skyHeight = h * horizon;

        skyPaint.setShader(new LinearGradient(0, 0, 0, skyHeight * 1.2f,
                SKY_TOP[theme], SKY_BOTTOM[theme], Shader.TileMode.CLAMP));
        cloudPaint.setColor(theme == 4 ? 0x66FFFFFF : 0xEBFFFFFF);

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

            // Each hill fades from its color to a slightly darker one.
            // The nearest hill fades into the page color, so the scene melts into the screen.
            int base = HILL_BASE[theme][i];
            int bottom = (i == HILLS - 1) ? CREAM : shade(base, 0.93f);
            float top = h * (horizon + i * 0.07f) - hillAmp(i, h) * 1.3f;
            hillPaints[i].setShader(new LinearGradient(0, top, 0, h + extra,
                    base, bottom, Shader.TileMode.CLAMP));
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
        drawSignature(canvas, w, h, time);

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

    // One small moving detail that tells you the topic
    private void drawSignature(Canvas canvas, float w, float h, float time) {
        switch (theme) {
            case 1: drawSteam(canvas, w, time); break;
            case 2: drawPlane(canvas, w, time); break;
            case 3: drawBalloons(canvas, w, time); break;
            case 4: drawLanterns(canvas, w, h, time); break;
            case 5: drawHotAirBalloon(canvas, w, time); break;
            case 6: drawSkyline(canvas, w, h); break;
            default: break;
        }
    }

    // Café: three wisps of steam rising and fading
    private void drawSteam(Canvas canvas, float w, float time) {
        float d = density;
        strokePaint.setStrokeWidth(3 * d);
        for (int i = 0; i < 3; i++) {
            float x = w * (0.22f + 0.28f * i);
            float rise = ((time * 12f + i * 20f) % 60f) * d;
            float fade = 1f - rise / (60f * d);
            float y = skyHeight * 0.95f - rise;

            strokePaint.setColor(0xFFFFFFFF);
            strokePaint.setAlpha((int) (170 * fade));
            tmpPath.reset();
            tmpPath.moveTo(x, y);
            tmpPath.cubicTo(x + 10 * d, y - 14 * d, x - 10 * d, y - 28 * d, x, y - 42 * d);
            canvas.drawPath(tmpPath, strokePaint);
        }
    }

    // Airport: a little plane crossing the sky, with a trail
    private void drawPlane(Canvas canvas, float w, float time) {
        float s = density;
        float span = w + 120 * s;
        float x = ((time * 16f * s) % span) - 60 * s;
        float y = skyHeight * 0.30f + (float) Math.sin(time * 0.6f) * 4 * s;

        strokePaint.setColor(0xFFFFFFFF);
        strokePaint.setAlpha(150);
        strokePaint.setStrokeWidth(2 * s);
        canvas.drawLine(x - 70 * s, y + 2 * s, x - 16 * s, y + 2 * s, strokePaint);

        accentPaint.setColor(0xFFFFFFFF);
        rect.set(x - 14 * s, y - 4 * s, x + 16 * s, y + 4 * s);
        canvas.drawRoundRect(rect, 4 * s, 4 * s, accentPaint);

        accentPaint.setColor(0xFFF4A259);
        tmpPath.reset();
        tmpPath.moveTo(x - 2 * s, y);
        tmpPath.lineTo(x - 12 * s, y + 14 * s);
        tmpPath.lineTo(x + 4 * s, y + 2 * s);
        tmpPath.close();
        canvas.drawPath(tmpPath, accentPaint);

        tmpPath.reset();
        tmpPath.moveTo(x - 12 * s, y - 2 * s);
        tmpPath.lineTo(x - 18 * s, y - 10 * s);
        tmpPath.lineTo(x - 8 * s, y - 2 * s);
        tmpPath.close();
        canvas.drawPath(tmpPath, accentPaint);
    }

    // Shop: four balloons floating up
    private void drawBalloons(Canvas canvas, float w, float time) {
        float d = density;
        for (int i = 0; i < 4; i++) {
            float x = w * (0.15f + 0.22f * i) + (float) Math.sin(time * 0.7f + i * 1.7f) * 6 * d;
            float travel = skyHeight * 0.9f;
            float y = skyHeight * 0.95f - ((time * (8f + i * 2f) * d + i * 40 * d) % travel);

            strokePaint.setColor(0x80606C74);
            strokePaint.setStrokeWidth(1.2f * d);
            canvas.drawLine(x, y + 12 * d, x + 2 * d, y + 30 * d, strokePaint);

            accentPaint.setColor(BALLOON_COLORS[i]);
            rect.set(x - 9 * d, y - 12 * d, x + 9 * d, y + 12 * d);
            canvas.drawOval(rect, accentPaint);
        }
    }

    // Festival: glowing lanterns drifting up from behind the hills
    private void drawLanterns(Canvas canvas, float w, float h, float time) {
        float d = density;
        for (int i = 0; i < 6; i++) {
            float x = w * (0.10f + 0.16f * i) + (float) Math.sin(time * 0.5f + i * 2f) * 8 * d;
            float travel = h * 0.9f;
            float y = h * 0.85f - ((time * (7f + i) * d + i * 55 * d) % travel);

            accentPaint.setColor(0x33FFC878);
            canvas.drawCircle(x, y, 24 * d, accentPaint);
            accentPaint.setColor(0x55FFB45A);
            canvas.drawCircle(x, y, 14 * d, accentPaint);

            accentPaint.setColor(0xFFFFA94D);
            rect.set(x - 7 * d, y - 10 * d, x + 7 * d, y + 10 * d);
            canvas.drawRoundRect(rect, 5 * d, 5 * d, accentPaint);
            accentPaint.setColor(0xFFFFE3A8);
            rect.set(x - 4 * d, y - 6 * d, x + 4 * d, y + 6 * d);
            canvas.drawRoundRect(rect, 3 * d, 3 * d, accentPaint);
        }
    }

    // Travel: a hot-air balloon drifting slowly across
    private void drawHotAirBalloon(Canvas canvas, float w, float time) {
        float d = density;
        float span = w + 100 * d;
        float x = ((time * 6f * d) % span) - 50 * d;
        float y = skyHeight * 0.40f + (float) Math.sin(time * 0.5f) * 5 * d;
        float r = 20 * d;

        strokePaint.setColor(0x99606C74);
        strokePaint.setStrokeWidth(1.2f * d);
        canvas.drawLine(x - 8 * d, y + r * 0.9f, x - 5 * d, y + r + 10 * d, strokePaint);
        canvas.drawLine(x + 8 * d, y + r * 0.9f, x + 5 * d, y + r + 10 * d, strokePaint);

        accentPaint.setColor(0xFF8A6A55);
        rect.set(x - 6 * d, y + r + 10 * d, x + 6 * d, y + r + 18 * d);
        canvas.drawRoundRect(rect, 2 * d, 2 * d, accentPaint);

        accentPaint.setColor(0xFFF29C7A);
        canvas.drawCircle(x, y, r, accentPaint);
        accentPaint.setColor(0xFFFFE3A8);
        rect.set(x - r * 0.35f, y - r, x + r * 0.35f, y + r);
        canvas.drawOval(rect, accentPaint);
    }

    // Business: a calm skyline behind the nearer hills
    private void drawSkyline(Canvas canvas, float w, float h) {
        float d = density;
        float[] heights = {40, 64, 48, 80, 56, 70, 44, 60, 36};
        float bw = w / heights.length;
        float baseY = skyHeight + h * 0.05f;

        for (int i = 0; i < heights.length; i++) {
            float bh = heights[i] * d;
            accentPaint.setColor(0xFFB3C3D1);
            rect.set(i * bw + 3 * d, baseY - bh, (i + 1) * bw - 3 * d, baseY + 60 * d);
            canvas.drawRect(rect, accentPaint);

            // two lit windows on each building
            accentPaint.setColor(0xFFFFF3C4);
            float cx = i * bw + bw / 2f;
            canvas.drawRect(cx - 5 * d, baseY - bh + 8 * d, cx - 1 * d, baseY - bh + 12 * d, accentPaint);
            canvas.drawRect(cx + 1 * d, baseY - bh + 16 * d, cx + 5 * d, baseY - bh + 20 * d, accentPaint);
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