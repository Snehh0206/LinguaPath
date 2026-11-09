package com.example.linguapath.view;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.DiscretePathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.linguapath.R;

// A storybook hero: an open book with a little sprout, on a mossy mound.
// Flat soft colours and a slightly wobbly brown outline, like a hand drawing.
// If you add your own painted logo as res/drawable/hero_logo.webp (or .png), it is used instead.
// Call startIdle() in onResume and stopIdle() in onPause.
public class HeroView extends View {

    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stemPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint detailPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path leftPage = new Path();
    private final Path rightPage = new Path();
    private final Path leafPath = new Path();
    private final RectF rect = new RectF();

    private final float density;
    private final int cardColor, sageColor, butterColor, leafColor, accentColor;
    private Bitmap painted; // your own painted logo, if you added one

    private float bookCx, bookCy, pageW, pageH, dip, thickness, floatDistance;
    private float floatPhase = 0f; // 0..1, drives the floating
    private float boost = 0f;      // extra sway after a tap

    private ValueAnimator floatAnimator;
    private ValueAnimator boostAnimator;
    private ObjectAnimator wobbleAnimator;

    public HeroView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        density = context.getResources().getDisplayMetrics().density;
        int ink = ContextCompat.getColor(context, R.color.ink);
        cardColor = ContextCompat.getColor(context, R.color.card);
        sageColor = ContextCompat.getColor(context, R.color.sage);
        butterColor = ContextCompat.getColor(context, R.color.butter);
        leafColor = ContextCompat.getColor(context, R.color.leaf);
        accentColor = ContextCompat.getColor(context, R.color.accent);

        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setFilterBitmap(true);

        // The outline wobbles a little, like a pencil line
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(2.2f * density);
        linePaint.setStrokeJoin(Paint.Join.ROUND);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setColor((ink & 0x00FFFFFF) | 0xD0000000);
        linePaint.setPathEffect(new DiscretePathEffect(9 * density, 1.3f * density));

        stemPaint.setStyle(Paint.Style.STROKE);
        stemPaint.setStrokeWidth(4 * density);
        stemPaint.setStrokeCap(Paint.Cap.ROUND);
        stemPaint.setColor(0xFF6E9E6A);

        detailPaint.setStyle(Paint.Style.STROKE);
        detailPaint.setStrokeWidth(1.6f * density);
        detailPaint.setStrokeCap(Paint.Cap.ROUND);
        detailPaint.setColor((ink & 0x00FFFFFF) | 0x55000000);

        int res = context.getResources().getIdentifier("hero_logo", "drawable", context.getPackageName());
        if (res != 0) {
            painted = BitmapFactory.decodeResource(context.getResources(), res);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        bookCx = w / 2f;
        bookCy = h * 0.56f;
        pageW = w * 0.30f;
        pageH = h * 0.26f;
        dip = h * 0.035f;
        thickness = h * 0.03f;
        floatDistance = h * 0.05f;
        buildPage(leftPage, -1);
        buildPage(rightPage, 1);
    }

    // side = -1 for the left page, +1 for the right page
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
        if (painted != null) {
            drawPainted(canvas, w, h);
            return;
        }

        // 1. A mossy mound. It stays on the ground while the book floats above it.
        fillPaint.setColor(sageColor);
        rect.set(w * 0.14f, h * 0.80f, w * 0.86f, h * 0.96f);
        canvas.drawOval(rect, fillPaint);
        canvas.drawOval(rect, linePaint);

        // 2. Everything that floats
        canvas.save();
        canvas.translate(0f, -floatDistance * floatPhase);

        // The cover, a little lower than the pages
        canvas.save();
        canvas.translate(0f, thickness);
        fillPaint.setColor(accentColor);
        canvas.drawPath(leftPage, fillPaint);
        canvas.drawPath(rightPage, fillPaint);
        canvas.drawPath(leftPage, linePaint);
        canvas.drawPath(rightPage, linePaint);
        canvas.restore();

        // The pages
        fillPaint.setColor(cardColor);
        canvas.drawPath(leftPage, fillPaint);
        canvas.drawPath(rightPage, fillPaint);
        canvas.drawPath(leftPage, linePaint);
        canvas.drawPath(rightPage, linePaint);

        // The spine and a few pencil lines of "writing"
        float top = bookCy - pageH / 2f;
        float bottom = bookCy + pageH / 2f;
        canvas.drawLine(bookCx, top + dip, bookCx, bottom + dip, detailPaint);
        for (int side = -1; side <= 1; side += 2) {
            for (int i = 0; i < 3; i++) {
                float y = bookCy - pageH * 0.18f + i * pageH * 0.18f;
                canvas.drawLine(bookCx + side * pageW * 0.18f, y + dip * 0.5f,
                        bookCx + side * pageW * 0.78f, y - dip * 0.5f, detailPaint);
            }
        }

        drawSprout(canvas);

        // A few tiny specks drifting up
        fillPaint.setColor(butterColor);
        for (int i = 0; i < 3; i++) {
            float x = bookCx + (i - 1) * pageW * 0.95f;
            float y = bookCy - pageH * (0.95f + 0.28f * i) - floatPhase * h * 0.02f * (i + 1);
            canvas.drawCircle(x, y, (2.2f + 0.6f * i) * density, fillPaint);
        }
        canvas.restore();
    }

    // A little sprout growing from the spine. It sways with the floating.
    private void drawSprout(Canvas canvas) {
        float baseX = bookCx;
        float baseY = bookCy - pageH / 2f + dip;
        float stem = pageH * 0.9f;
        float sway = (floatPhase - 0.5f) * 8f
                + boost * 10f * (float) Math.sin(floatPhase * 6.28f * 3f);

        canvas.save();
        canvas.rotate(sway, baseX, baseY);
        canvas.drawLine(baseX, baseY, baseX, baseY - stem, stemPaint);
        drawLeaf(canvas, baseX, baseY - stem * 0.55f, pageH * 0.55f, -48f);
        drawLeaf(canvas, baseX, baseY - stem, pageH * 0.62f, 38f);
        drawLeaf(canvas, baseX, baseY - stem, pageH * 0.42f, -12f);
        canvas.restore();
    }

    private void drawLeaf(Canvas canvas, float x, float y, float size, float angle) {
        canvas.save();
        canvas.translate(x, y);
        canvas.rotate(angle);
        leafPath.reset();
        leafPath.moveTo(0, 0);
        leafPath.quadTo(-size * 0.45f, -size * 0.5f, 0, -size);
        leafPath.quadTo(size * 0.45f, -size * 0.5f, 0, 0);
        fillPaint.setColor(leafColor);
        canvas.drawPath(leafPath, fillPaint);
        canvas.drawPath(leafPath, linePaint);
        canvas.restore();
    }

    private void drawPainted(Canvas canvas, float w, float h) {
        float side = Math.min(w, h * 0.92f);
        float top = h * 0.04f - floatDistance * floatPhase;
        rect.set(w / 2f - side / 2f, top, w / 2f + side / 2f, top + side);
        canvas.drawBitmap(painted, null, rect, fillPaint);
    }

    // ---- Idle: gentle floating loop ----

    public void startIdle() {
        if (floatAnimator != null) {
            return; // never start twice
        }
        floatAnimator = ValueAnimator.ofFloat(0f, 1f);
        floatAnimator.setDuration(2600);
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
        boost = 0f;
        invalidate();
    }

    // ---- Tap: a small pop, a wobble, and the sprout sways ----

    public void react() {
        animate().cancel();
        animate().scaleX(1.1f).scaleY(1.1f).setDuration(120)
                .withEndAction(() -> animate().scaleX(1f).scaleY(1f).setDuration(350)
                        .setInterpolator(new OvershootInterpolator(3f)));

        if (wobbleAnimator != null) {
            wobbleAnimator.cancel();
        }
        wobbleAnimator = ObjectAnimator.ofFloat(this, View.ROTATION, 0f, -6f, 5f, -3f, 0f);
        wobbleAnimator.setDuration(600);
        wobbleAnimator.start();

        if (boostAnimator != null) {
            boostAnimator.cancel();
        }
        boostAnimator = ValueAnimator.ofFloat(1f, 0f);
        boostAnimator.setDuration(900);
        boostAnimator.addUpdateListener(a -> {
            boost = (float) a.getAnimatedValue();
            invalidate();
        });
        boostAnimator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        stopIdle();
        super.onDetachedFromWindow();
    }
}