package com.nothing.player;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class DotMatrixProgressBar extends View {
    private static final int DEFAULT_TOTAL_DOTS = 14;

    private int progress = 50; // 0 to 100 or up to 200 (boost)
    private int max = 100;
    private int totalDots = DEFAULT_TOTAL_DOTS;

    private int normalActiveColor = Color.WHITE;
    private int boostActiveColor = Color.parseColor("#D71921");
    private int inactiveColor = Color.parseColor("#22FFFFFF");

    private final Paint paintActive = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintBoost = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintInactive = new Paint(Paint.ANTI_ALIAS_FLAG);

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
        paintActive.setColor(normalActiveColor);

        paintBoost.setStyle(Paint.Style.FILL);
        paintBoost.setColor(boostActiveColor);

        paintInactive.setStyle(Paint.Style.FILL);
        paintInactive.setColor(inactiveColor);
    }

    public void setProgress(int progress) {
        this.progress = Math.max(0, progress);
        invalidate();
    }

    public void setMax(int max) {
        this.max = Math.max(1, max);
        invalidate();
    }

    public void setTotalDots(int dots) {
        this.totalDots = Math.max(5, dots);
        invalidate();
    }

    public void setNormalActiveColor(int color) {
        this.normalActiveColor = color;
        paintActive.setColor(color);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        if (width == 0 || height == 0 || totalDots == 0) return;

        float spacing = (float) width / totalDots;
        float dotDiameter = Math.min(spacing * 0.65f, height * 0.75f);
        float dotRadius = dotDiameter / 2f;
        float cy = height / 2f;

        // Calculate how many dots are active
        float ratio = (float) progress / (float) max;
        int activeCount = Math.round(ratio * totalDots);

        for (int i = 0; i < totalDots; i++) {
            float cx = (i * spacing) + (spacing / 2f);

            if (i < activeCount) {
                // If progress > 100% (boost mode) or Red themed
                if (progress > max || normalActiveColor == boostActiveColor) {
                    canvas.drawCircle(cx, cy, dotRadius, paintBoost);
                } else {
                    canvas.drawCircle(cx, cy, dotRadius, paintActive);
                }
            } else {
                canvas.drawCircle(cx, cy, dotRadius, paintInactive);
            }
        }
    }
}
