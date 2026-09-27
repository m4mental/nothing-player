package com.nothing.player;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class DotMatrixProgressBar extends View {
    private static final int DEFAULT_TOTAL_DOTS = 0; // 0 means calculate dynamically based on density

    private int progress = 0;
    private int max = 100;
    private int totalDots = DEFAULT_TOTAL_DOTS;

    private int activeColor = Color.parseColor("#D71921"); // Nothing Signature Red
    private int inactiveColor = Color.parseColor("#26FFFFFF"); // Subtle Dim Matrix Dots
    private int trackBgColor = Color.parseColor("#B3000000"); // 70% Dark Translucent Backdrop
    private boolean showInactiveDots = true;

    private final Paint paintActive = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintInactive = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintBackground = new Paint(Paint.ANTI_ALIAS_FLAG);

    public DotMatrixProgressBar(Context context) {
        super(context);
        init();
    }

    public DotMatrixProgressBar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DotMatrixProgressBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paintActive.setStyle(Paint.Style.FILL);
        paintActive.setColor(activeColor);

        paintInactive.setStyle(Paint.Style.FILL);
        paintInactive.setColor(inactiveColor);

        paintBackground.setStyle(Paint.Style.FILL);
        paintBackground.setColor(trackBgColor);
    }

    public void setProgress(int progress) {
        this.progress = Math.max(0, progress);
        invalidate();
    }

    public int getProgress() {
        return progress;
    }

    public void setMax(int max) {
        this.max = Math.max(1, max);
        invalidate();
    }

    public int getMax() {
        return max;
    }

    public void setTotalDots(int dots) {
        this.totalDots = Math.max(0, dots);
        invalidate();
    }

    public void setActiveColor(int color) {
        this.activeColor = color;
        paintActive.setColor(color);
        invalidate();
    }

    public void setInactiveColor(int color) {
        this.inactiveColor = color;
        paintInactive.setColor(color);
        invalidate();
    }

    public void setTrackBackgroundColor(int color) {
        this.trackBgColor = color;
        paintBackground.setColor(color);
        invalidate();
    }

    public void setShowInactiveDots(boolean show) {
        this.showInactiveDots = show;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) return;

        // 1. Draw subtle dark LED strip backing
        canvas.drawRect(0, 0, width, height, paintBackground);

        // 2. Calculate authentic Nothing OS micro-dot pitch
        float density = getResources().getDisplayMetrics().density;
        float dotPitch = 4.5f * density; // ~4.5dp spacing between dots
        int numDots = totalDots > 0 ? totalDots : Math.max(10, (int) (width / dotPitch));
        if (numDots <= 0) return;

        float spacing = (float) width / (float) numDots;
        float dotDiameter = Math.min(spacing * 0.62f, height * 0.72f);
        float dotRadius = dotDiameter / 2f;
        float cy = height / 2f;

        // 3. Calculate active dots count
        float ratio = Math.max(0f, Math.min(1f, (float) progress / (float) max));
        int activeCount = (int) Math.round(ratio * numDots);
        if (progress > 0 && activeCount == 0) activeCount = 1; // Show at least 1 dot if playback started

        // 4. Draw dot matrix line
        for (int i = 0; i < numDots; i++) {
            float cx = (i * spacing) + (spacing / 2f);

            if (i < activeCount) {
                canvas.drawCircle(cx, cy, dotRadius, paintActive);
            } else if (showInactiveDots) {
                canvas.drawCircle(cx, cy, dotRadius, paintInactive);
            }
        }
    }
}
