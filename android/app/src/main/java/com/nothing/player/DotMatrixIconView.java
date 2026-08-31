package com.nothing.player;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class DotMatrixIconView extends View {
    public static final int TYPE_BRIGHTNESS = 1;
    public static final int TYPE_VOLUME = 2;
    public static final int TYPE_VOLUME_MUTE = 3;
    public static final int TYPE_AUDIO_BOOST = 4;
    public static final int TYPE_SEEK_FORWARD = 5;
    public static final int TYPE_SEEK_REWIND = 6;
    public static final int TYPE_LOCK = 7;

    private int iconType = TYPE_VOLUME;
    private int activeColor = Color.WHITE;
    private int inactiveColor = Color.parseColor("#18FFFFFF");

    private final Paint activePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint inactivePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private static final int GRID_SIZE = 9;

    // 9x9 Bitmaps (1 = active LED dot, 0 = dim inactive LED dot)
    private static final int[][] PATTERN_BRIGHTNESS = {
        {0, 0, 0, 0, 1, 0, 0, 0, 0},
        {0, 1, 0, 0, 0, 0, 0, 1, 0},
        {0, 0, 0, 1, 1, 1, 0, 0, 0},
        {0, 0, 1, 1, 1, 1, 1, 0, 0},
        {1, 0, 1, 1, 1, 1, 1, 0, 1},
        {0, 0, 1, 1, 1, 1, 1, 0, 0},
        {0, 0, 0, 1, 1, 1, 0, 0, 0},
        {0, 1, 0, 0, 0, 0, 0, 1, 0},
        {0, 0, 0, 0, 1, 0, 0, 0, 0}
    };

    private static final int[][] PATTERN_VOLUME = {
        {0, 0, 0, 1, 0, 0, 0, 1, 0},
        {0, 0, 1, 1, 0, 1, 0, 0, 1},
        {0, 1, 1, 1, 0, 0, 1, 0, 1},
        {1, 1, 1, 1, 0, 0, 1, 0, 1},
        {1, 1, 1, 1, 0, 0, 1, 0, 1},
        {1, 1, 1, 1, 0, 0, 1, 0, 1},
        {0, 1, 1, 1, 0, 0, 1, 0, 1},
        {0, 0, 1, 1, 0, 1, 0, 0, 1},
        {0, 0, 0, 1, 0, 0, 0, 1, 0}
    };

    private static final int[][] PATTERN_VOLUME_MUTE = {
        {1, 0, 0, 1, 0, 0, 0, 0, 1},
        {0, 1, 1, 1, 0, 0, 0, 1, 0},
        {0, 0, 1, 1, 0, 0, 1, 0, 0},
        {1, 1, 1, 1, 0, 1, 0, 0, 0},
        {1, 1, 1, 1, 1, 0, 0, 0, 0},
        {1, 1, 1, 1, 0, 1, 0, 0, 0},
        {0, 1, 1, 1, 0, 0, 1, 0, 0},
        {0, 0, 1, 1, 0, 0, 0, 1, 0},
        {0, 0, 0, 1, 0, 0, 0, 0, 1}
    };

    private static final int[][] PATTERN_AUDIO_BOOST = {
        {0, 0, 0, 1, 1, 0, 0, 1, 0},
        {0, 0, 1, 1, 0, 0, 1, 1, 0},
        {0, 1, 1, 1, 0, 1, 1, 0, 0},
        {1, 1, 1, 1, 1, 1, 1, 1, 1},
        {1, 1, 1, 1, 1, 1, 1, 1, 1},
        {0, 0, 0, 0, 1, 1, 0, 0, 0},
        {0, 0, 0, 1, 1, 0, 0, 0, 0},
        {0, 0, 1, 1, 0, 0, 0, 0, 0},
        {0, 1, 1, 0, 0, 0, 0, 0, 0}
    };

    private static final int[][] PATTERN_SEEK_FORWARD = {
        {0, 1, 0, 0, 0, 1, 0, 0, 0},
        {0, 1, 1, 0, 0, 1, 1, 0, 0},
        {0, 1, 1, 1, 0, 1, 1, 1, 0},
        {0, 1, 1, 1, 1, 1, 1, 1, 1},
        {0, 1, 1, 1, 0, 1, 1, 1, 0},
        {0, 1, 1, 0, 0, 1, 1, 0, 0},
        {0, 1, 0, 0, 0, 1, 0, 0, 0},
        {0, 0, 0, 0, 0, 0, 0, 0, 0},
        {0, 0, 0, 0, 0, 0, 0, 0, 0}
    };

    private static final int[][] PATTERN_SEEK_REWIND = {
        {0, 0, 0, 1, 0, 0, 0, 1, 0},
        {0, 0, 1, 1, 0, 0, 1, 1, 0},
        {0, 1, 1, 1, 0, 1, 1, 1, 0},
        {1, 1, 1, 1, 1, 1, 1, 1, 0},
        {0, 1, 1, 1, 0, 1, 1, 1, 0},
        {0, 0, 1, 1, 0, 0, 1, 1, 0},
        {0, 0, 0, 1, 0, 0, 0, 1, 0},
        {0, 0, 0, 0, 0, 0, 0, 0, 0},
        {0, 0, 0, 0, 0, 0, 0, 0, 0}
    };

    private static final int[][] PATTERN_LOCK = {
        {0, 0, 1, 1, 1, 1, 1, 0, 0},
        {0, 1, 0, 0, 0, 0, 0, 1, 0},
        {0, 1, 0, 0, 0, 0, 0, 1, 0},
        {1, 1, 1, 1, 1, 1, 1, 1, 1},
        {1, 1, 1, 1, 1, 1, 1, 1, 1},
        {1, 1, 1, 0, 1, 0, 1, 1, 1},
        {1, 1, 1, 0, 1, 0, 1, 1, 1},
        {1, 1, 1, 1, 1, 1, 1, 1, 1},
        {1, 1, 1, 1, 1, 1, 1, 1, 1}
    };

    public DotMatrixIconView(Context context) {
        super(context);
        init();
    }

    public DotMatrixIconView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DotMatrixIconView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        activePaint.setStyle(Paint.Style.FILL);
        activePaint.setColor(activeColor);
        inactivePaint.setStyle(Paint.Style.FILL);
        inactivePaint.setColor(inactiveColor);
    }

    public void setIconType(int type) {
        this.iconType = type;
        if (type == TYPE_VOLUME_MUTE) {
            this.activeColor = Color.parseColor("#D71921");
        } else if (type == TYPE_AUDIO_BOOST) {
            this.activeColor = Color.parseColor("#D71921");
        } else {
            this.activeColor = Color.WHITE;
        }
        activePaint.setColor(activeColor);
        invalidate();
    }

    public void setActiveColor(int color) {
        this.activeColor = color;
        activePaint.setColor(color);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        if (width == 0 || height == 0) return;

        float size = Math.min(width, height);
        float dotDiameter = size / (GRID_SIZE * 1.5f);
        float dotRadius = dotDiameter / 2f;
        float spacing = size / GRID_SIZE;
        float offsetX = (width - size) / 2f + spacing / 2f;
        float offsetY = (height - size) / 2f + spacing / 2f;

        int[][] matrix = getMatrixForType(iconType);

        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                float cx = offsetX + col * spacing;
                float cy = offsetY + row * spacing;

                boolean isActive = (row < matrix.length && col < matrix[row].length) && (matrix[row][col] == 1);
                canvas.drawCircle(cx, cy, dotRadius, isActive ? activePaint : inactivePaint);
            }
        }
    }

    private int[][] getMatrixForType(int type) {
        switch (type) {
            case TYPE_BRIGHTNESS: return PATTERN_BRIGHTNESS;
            case TYPE_VOLUME_MUTE: return PATTERN_VOLUME_MUTE;
            case TYPE_AUDIO_BOOST: return PATTERN_AUDIO_BOOST;
            case TYPE_SEEK_FORWARD: return PATTERN_SEEK_FORWARD;
            case TYPE_SEEK_REWIND: return PATTERN_SEEK_REWIND;
            case TYPE_LOCK: return PATTERN_LOCK;
            case TYPE_VOLUME:
            default:
                return PATTERN_VOLUME;
        }
    }
}
