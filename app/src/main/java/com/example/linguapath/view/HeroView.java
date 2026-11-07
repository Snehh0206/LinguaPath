package com.example.linguapath.view;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.linguapath.R;

// A floating open book with a soft halo and a ground shadow.
// The shadow shrinks and fades as the book rises, which gives the 3D "floating" feel.
// Drop it into any layout. Call startIdle() in onResume and stopIdle() in onPause.
public class HeroView extends View {

    private final Paint haloPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint coverPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint leftPagePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rightPagePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint spinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ribbonPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sparklePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Path leftPage = new Path();
    private final Path rightPage = new Path();
    private final Path ribbon = new Path();
    private final Path star = new Path();

    private final float density;
    private final int primary;
    private final int primaryDark;
    private final int accent;
    private final int inkColor;

    private float bookCx, bookCy, pageW, pageH, dip, thickness, floatDistance;
    private float floatPhase = 0f;    // 0..1, drives float, shadow and sparkles
    private float sparkleBoost = 0f;  // extra sparkle after a tap

    private ValueAnimator floatAnimator;
    private ValueAnimator boostAnimator;
    private ObjectAnimator wobbleAnimator;

    public HeroView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        density = context.getResources().getDisplayMetrics().density;
        primary = ContextCompat.getColor(context, R.color.primary);
        primaryDark = ContextCompat.getColor(context, R.color.primary_dark);
        accent = ContextCompat.getColor(context, R.color.accent);
        inkColor = ContextCompat.getColor(context, R.color.ink);

        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeWidth(2 * density);
        linePaint.setColor(withAlpha(ContextCompat.getColor(context, R.color.ink_soft), 70));

        spinePaint.setStyle(Paint.Style.STROKE);
        spinePaint.setStrokeWidth(1.5f * density);
        spinePaint.setColor(withAlpha(inkColor, 40));

        sparklePaint.setStyle(Paint.Style.FILL);
    }

    private static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        bookCx = w / 2f;
        bookCy = h * 0.46f;
        pageW = w * 0.38f;
        pageH = h * 0.36f;
        dip = h * 0.05f;
        thickness = h * 0.035f;
        floatDistance = h * 0.06f;

        buildPage(leftPage, -1);
        buildPage(rightPage, 1);

        float top = bookCy - pageH / 2f;
        float bottom = bookCy + pageH / 2f + dip + thickness;

        haloPaint.setShader(new RadialGradient(bookCx, bookCy, w * 0.48f,
                new int[]{withAlpha(ContextCompat.getColor(getContext(), R.color.sage), 170),
                        withAlpha(ContextCompat.getColor(getContext(), R.color.sage), 0)},
                null, Shader.TileMode.CLAMP));

        shadowPaint.setShader(new RadialGradient(bookCx, h * 0.90f, w * 0.34f,
                new int[]{withAlpha(inkColor, 90), withAlpha(inkColor, 0)},
                null, Shader.TileMode.CLAMP));

        coverPaint.setShader(new LinearGradient(0, top, 0, bottom, primary, primaryDark,
                Shader.TileMode.CLAMP));

        int pageWhite = Color.WHITE;
        int pageShade = 0xFFDDE3E5;
        leftPagePaint.setShader(new LinearGradient(bookCx - pageW, 0, bookCx, 0,
                pageWhite, pageShade, Shader.TileMode.CLAMP));
        rightPagePaint.setShader(new LinearGradient(bookCx + pageW, 0, bookCx, 0,
                pageWhite, pageShade, Shader.TileMode.CLAMP));

        // Bookmark ribbon hanging from the spine
        float ribbonTop = bookCy + pageH / 2f + dip - h * 0.01f;
        float half = w * 0.025f;
        float ribbonBottom = ribbonTop + h * 0.11f;
        ribbon.reset();
        ribbon.moveTo(bookCx - half, ribbonTop);
        ribbon.lineTo(bookCx + half, ribbonTop);
        ribbon.lineTo(bookCx + half, ribbonBottom);
        ribbon.lineTo(bookCx, ribbonBottom - h * 0.03f);
        ribbon.lineTo(bookCx - half, ribbonBottom);
        ribbon.close();
        ribbonPaint.setShader(new LinearGradient(0, ribbonTop, 0, ribbonBottom,
                0xFFF9BE82, accent, Shader.TileMode.CLAMP));
    }

    // side = -1 for the left page, +1 for the right page.
    // The outer edges sit a little higher than the spine, like an open book seen from the front.
    private void buildPage(Path p, int side) {
        float top = bookCy - pageH / 2f;
        float bottom = bookCy + pageH / 2f;
        float outerX = bookCx + side * pageW;
        p.reset();
        p.moveTo(bookCx, top + dip);
        p.quadTo(bookCx + side * pageW * 0.5f, top - dip * 0.4f, outerX, top - dip);
        p.lineTo(outerX, bottom - dip);
        p.quadTo(bookCx + side * pageW * 0.5f, bottom + dip * 0.4f, bookCx, bottom + dip);
        p.close();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        if (w == 0f) {
            return;
        }

        // Ground shadow: stays on the ground, gets smaller and lighter as the book rises
        float shadowScale = 1f - 0.22f * floatPhase;
        shadowPaint.setAlpha((int) (255 * (1f - 0.4f * floatPhase)));
        canvas.save();
        canvas.scale(shadowScale, 0.28f * shadowScale, bookCx, h * 0.90f);
        canvas.drawCircle(bookCx, h * 0.90f, w * 0.34f, shadowPaint);
        canvas.restore();

        // The book floats up and down
        canvas.save();
        canvas.translate(0f, -floatDistance * floatPhase);

        canvas.drawCircle(bookCx, bookCy, w * 0.48f, haloPaint);

        // Cover, slightly lower than the pages so it reads as thickness
        canvas.save();
        canvas.translate(0f, thickness);
        canvas.drawPath(leftPage, coverPaint);
        canvas.drawPath(rightPage, coverPaint);
        canvas.restore();

        canvas.drawPath(leftPage, leftPagePaint);
        canvas.drawPath(rightPage, rightPagePaint);

        float top = bookCy - pageH / 2f;
        float bottom = bookCy + pageH / 2f;
        canvas.drawLine(bookCx, top + dip, bookCx, bottom + dip, spinePaint);

        drawTextLines(canvas, -1);
        drawTextLines(canvas, 1);

        canvas.drawPath(ribbon, ribbonPaint);

        // Sparkles that gently pulse
        drawSparkle(canvas, bookCx - pageW * 1.05f, bookCy - pageH * 0.95f, h * 0.05f, 0f);
        drawSparkle(canvas, bookCx + pageW * 0.95f, bookCy - pageH * 0.80f, h * 0.04f, 1.6f);
        drawSparkle(canvas, bookCx + pageW * 0.15f, bookCy - pageH * 1.20f, h * 0.03f, 3.1f);

        canvas.restore();
    }

    private void drawTextLines(Canvas canvas, int side) {
        for (int i = 0; i < 3; i++) {
            float y = bookCy - pageH * 0.18f + i * pageH * 0.18f;
            float x1 = bookCx + side * pageW * 0.18f;
            float x2 = bookCx + side * pageW * 0.80f;
            canvas.drawLine(x1, y + dip * 0.5f, x2, y - dip * 0.5f, linePaint);
        }
    }

    private void drawSparkle(Canvas canvas, float x, float y, float size, float offset) {
        float pulse = 0.5f + 0.5f * (float) Math.sin(floatPhase * Math.PI + offset);
        float s = size * (0.7f + 0.5f * pulse + sparkleBoost);
        int alpha = (int) Math.min(255, 110 + 120 * pulse + 120 * sparkleBoost);
        sparklePaint.setColor(withAlpha(offset > 1f ? accent : primary, alpha));

        star.reset();
        star.moveTo(x, y - s);
        star.quadTo(x, y, x + s, y);
        star.quadTo(x, y, x, y + s);
        star.quadTo(x, y, x - s, y);
        star.quadTo(x, y, x, y - s);
        star.close();
        canvas.drawPath(star, sparklePaint);
    }

    // ---- Idle: gentle floating loop ----

    public void startIdle() {
        if (floatAnimator != null) {
            return; // never start twice
        }
        floatAnimator = ValueAnimator.ofFloat(0f, 1f);
        floatAnimator.setDuration(2400);
        floatAnimator.setRepeatCount(ValueAnimator.INFINITE);
        floatAnimator.setRepeatMode(ValueAnimator.REVERSE);
        floatAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        floatAnimator.addUpdateListener(a -> {
            floatPhase = (float) a.getAnimatedValue();
            invalidate();
        });
        floatAnimator.start();
    }

    public void stopIdle() {
        if (floatAnimator != null) {
            floatAnimator.cancel();
            floatAnimator = null;
        }
        if (boostAnimator != null) {
            boostAnimator.cancel();
        }
        if (wobbleAnimator != null) {
            wobbleAnimator.cancel();
        }
        animate().cancel();
        setScaleX(1f);
        setScaleY(1f);
        setRotation(0f);
        floatPhase = 0f;
        sparkleBoost = 0f;
        invalidate();
    }

    // ---- Tap reaction: a small pop, a wobble, and a burst of sparkle ----

    public void react() {
        animate().cancel();
        animate().scaleX(1.12f).scaleY(1.12f).setDuration(120)
                .withEndAction(() -> animate().scaleX(1f).scaleY(1f).setDuration(350)
                        .setInterpolator(new OvershootInterpolator(3f)));

        if (wobbleAnimator != null) {
            wobbleAnimator.cancel();
        }
        wobbleAnimator = ObjectAnimator.ofFloat(this, View.ROTATION, 0f, -7f, 5f, -3f, 0f);
        wobbleAnimator.setDuration(600);
        wobbleAnimator.start();

        if (boostAnimator != null) {
            boostAnimator.cancel();
        }
        boostAnimator = ValueAnimator.ofFloat(1f, 0f);
        boostAnimator.setDuration(800);
        boostAnimator.addUpdateListener(a -> {
            sparkleBoost = (float) a.getAnimatedValue();
            invalidate();
        });
        boostAnimator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        stopIdle(); // nothing keeps running after the screen is gone
        super.onDetachedFromWindow();
    }
}