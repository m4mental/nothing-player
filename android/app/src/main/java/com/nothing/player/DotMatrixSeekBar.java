package com.nothing.player;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatSeekBar;

public class DotMatrixSeekBar extends AppCompatSeekBar {
    private final Paint paintActive = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintInactive = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintThumbOuter = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintThumbInner = new Paint(Paint.ANTI_ALIAS_FLAG);

    private int activeColor = Color.parseColor("#D71921"); // Nothing Red
    private int inactiveColor = Color.parseColor("#26FFFFFF"); // Faint LED dots
    private int thumbColor = Color.parseColor("#D71921");
    private int thumbDotColor = Color.WHITE;

    private static final int DOT_COUNT = 32;

    public DotMatrixSeekBar(Context context) {
        super(context);
        init();
    }

    public DotMatrixSeekBar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DotMatrixSeekBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paintActive.setStyle(Paint.Style.FILL);
        paintActive.setColor(activeColor);

        paintInactive.setStyle(Paint.Style.FILL);
        paintInactive.setColor(inactiveColor);

        paintThumbOuter.setStyle(Paint.Style.FILL);
        paintThumbOuter.setColor(thumbColor);

        paintThumbInner.setStyle(Paint.Style.FILL);
        paintThumbInner.setColor(thumbDotColor);

        // Hide default track & thumb drawing to replace with custom LED dot matrix
        setThumb(null);
    }

    public void setActiveColor(int color) {
        this.activeColor = color;
        paintActive.setColor(color);
        invalidate();
    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) return;

        int paddingL = getPaddingLeft();
        int paddingR = getPaddingRight();
        float availableWidth = width - paddingL - paddingR;
        float cy = height / 2f;

        float maxVal = Math.max(1, getMax());
        float progressVal = Math.max(0, Math.min(maxVal, getProgress()));
        float ratio = progressVal / maxVal;
        float thumbX = paddingL + (ratio * availableWidth);

        // 1. Draw Discrete High-Density Micro-LED Dot Matrix Track
        // Tightly spaced micro-dots for authentic Nothing OS Dot Matrix aesthetic
        float density = getResources().getDisplayMetrics().density;
        float dotSpacing = 5.2f * density; // Paas paas dots (tight ~5dp pitch)
        int dotCount = Math.max(10, (int) (availableWidth / dotSpacing));
        float actualSpacing = availableWidth / (float) dotCount;
        float dotRadius = 1.15f * density; // Micro-LED dots (small, crisp, authentic)

        for (int i = 0; i <= dotCount; i++) {
            float dotX = paddingL + (i * actualSpacing);
            if (dotX <= thumbX) {
                canvas.drawCircle(dotX, cy, dotRadius, paintActive);
            } else {
                canvas.drawCircle(dotX, cy, dotRadius, paintInactive);
            }
        }

        // 2. Draw Connected Hairline LED Center Track
        paintActive.setStrokeWidth(1.0f * density);
        paintActive.setStyle(Paint.Style.STROKE);
        canvas.drawLine(paddingL, cy, thumbX, cy, paintActive);
        paintActive.setStyle(Paint.Style.FILL);

        // 3. Draw Nothing OS Micro Scrubber Thumb (Clean concentric circular LED)
        float thumbRadiusOuter = 5.5f * density;
        float thumbRadiusInner = 2.2f * density;

        canvas.drawCircle(thumbX, cy, thumbRadiusOuter, paintThumbOuter);
        canvas.drawCircle(thumbX, cy, thumbRadiusInner, paintThumbInner);
    }
}
