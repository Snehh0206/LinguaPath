package com.example.linguapath.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;

import com.example.linguapath.R;
import com.example.linguapath.data.ProgressStore;
import com.example.linguapath.service.StudyTimerController;
import com.example.linguapath.service.StudyTimerService;
import com.example.linguapath.service.StudyTimerState;

// The oval timer at the bottom right. Tap it to slide it in; tap again to slide it out.
// When it is out, 20% of it stays visible. It follows the study timer by itself.
public class TimerPillView extends FrameLayout {

    private static final float HIDDEN_FRACTION = 0.8f; // 80% hidden, 20% visible

    // Remembered, so the pill looks the same when you move between screens
    private static boolean expanded = false;

    private final TextView textMain;
    private final TextView textSub;
    private final TextView buttonToggle;
    private final Observer<StudyTimerState.Snapshot> observer = this::render;

    public TimerPillView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(context).inflate(R.layout.view_timer_pill, this, true);
        textMain = findViewById(R.id.textPillMain);
        textSub = findViewById(R.id.textPillSub);
        buttonToggle = findViewById(R.id.buttonPillToggle);

        setBackgroundResource(R.drawable.bg_timer_pill);
        setElevation(8 * context.getResources().getDisplayMetrics().density);
        setClickable(true);
        setOnClickListener(v -> setExpanded(!expanded));
        buttonToggle.setOnClickListener(v -> StudyTimerController.togglePause());
    }

    private void setExpanded(boolean expand) {
        expanded = expand;
        animate().translationX(expand ? 0f : getWidth() * HIDDEN_FRACTION)
                .setDuration(250)
                .setInterpolator(new DecelerateInterpolator());
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        setTranslationX(expanded ? 0f : w * HIDDEN_FRACTION); // the starting position
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        StudyTimerState.live().observeForever(observer);
    }

    @Override
    protected void onDetachedFromWindow() {
        StudyTimerState.live().removeObserver(observer);
        super.onDetachedFromWindow();
    }

    private void render(StudyTimerState.Snapshot s) {
        long today = new ProgressStore(getContext()).getStudyMillisToday() + (s.active ? s.unsavedMs : 0);
        int minutes = (int) (today / 60000);

        if (s.active) {
            textMain.setText(StudyTimerService.format(s.sessionMs));
            textSub.setText(getContext().getString(
                    s.running ? R.string.pill_sub_running : R.string.pill_sub_paused, minutes));
            buttonToggle.setVisibility(View.VISIBLE);
            buttonToggle.setText(s.running ? "⏸" : "▶");
        } else {
            textMain.setText(getContext().getString(R.string.pill_today_main, minutes));
            textSub.setText(R.string.pill_today_sub);
            buttonToggle.setVisibility(View.GONE);
        }
    }
}