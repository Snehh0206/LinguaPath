package com.example.linguapath.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.linguapath.R;

// Draws a curved rope from one circle to the next one on the path
public class RopeView extends View {

    public static final int LEFT = 0;
    public static final int CENTER = 1;
    public static final int RIGHT = 2;

    // These must match item_stage.xml (side padding and node width)
    private static final float SIDE_PADDING_DP = 32f;
    private static final float NODE_WIDTH_DP = 120f;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private float density;
    private int fromPosition = CENTER;
    private int toPosition = CENTER;

    public RopeView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        density = context.getResources().getDisplayMetrics().density;

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(4 * density);
        paint.setColor(ContextCompat.getColor(context, R.color.ink));
    }

    // from / to are LEFT, CENTER or RIGHT. done = solid rope, not done = dotted rope.
    public void setRope(int from, int to, boolean done) {
        fromPosition = from;
        toPosition = to;
        if (done) {
            paint.setPathEffect(null);
        } else {
            paint.setPathEffect(new DashPathEffect(new float[]{4 * density, 10 * density}, 0));
        }
        invalidate();
    }

    // Horizontal middle of a circle in the given position
    private float centerX(int position, int width) {
        float side = SIDE_PADDING_DP * density;
        float half = NODE_WIDTH_DP * density / 2f;
        if (position == LEFT) return side + half;
        if (position == RIGHT) return width - side - half;
        return width / 2f;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();

        float startX = centerX(fromPosition, width);
        float endX = centerX(toPosition, width);

        // S-shaped curve from the top of this view to the bottom
        path.reset();
        path.moveTo(startX, 0);
        path.cubicTo(startX, height * 0.5f, endX, height * 0.5f, endX, height);
        canvas.drawPath(path, paint);
    }
}