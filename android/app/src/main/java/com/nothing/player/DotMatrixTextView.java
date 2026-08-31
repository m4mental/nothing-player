package com.nothing.player;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

public class DotMatrixTextView extends View {
    private static final int FONT_WIDTH = 5;
    private static final int FONT_HEIGHT = 7;

    private String text = "50%";
    private int textColor = Color.WHITE;
    private int inactiveDotColor = Color.parseColor("#15FFFFFF");

    private final Paint activePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint inactivePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private static final Map<Character, int[][]> FONT_MAP = new HashMap<>();

    static {
        FONT_MAP.put('0', new int[][]{
            {0, 1, 1, 1, 0},
            {1, 0, 0, 0, 1},
            {1, 0, 0, 1, 1},
            {1, 0, 1, 0, 1},
            {1, 1, 0, 0, 1},
            {1, 0, 0, 0, 1},
            {0, 1, 1, 1, 0}
        });

        FONT_MAP.put('1', new int[][]{
            {0, 0, 1, 0, 0},
            {0, 1, 1, 0, 0},
            {0, 0, 1, 0, 0},
            {0, 0, 1, 0, 0},
            {0, 0, 1, 0, 0},
            {0, 0, 1, 0, 0},
            {0, 1, 1, 1, 0}
        });

        FONT_MAP.put('2', new int[][]{
            {0, 1, 1, 1, 0},
            {1, 0, 0, 0, 1},
            {0, 0, 0, 0, 1},
            {0, 0, 1, 1, 0},
            {0, 1, 0, 0, 0},
            {1, 0, 0, 0, 0},
            {1, 1, 1, 1, 1}
        });

        FONT_MAP.put('3', new int[][]{
            {1, 1, 1, 1, 0},
            {0, 0, 0, 0, 1},
            {0, 0, 0, 0, 1},
            {0, 1, 1, 1, 0},
            {0, 0, 0, 0, 1},
            {0, 0, 0, 0, 1},
            {1, 1, 1, 1, 0}
        });

        FONT_MAP.put('4', new int[][]{
            {1, 0, 0, 0, 1},
            {1, 0, 0, 0, 1},
            {1, 0, 0, 0, 1},
            {1, 1, 1, 1, 1},
            {0, 0, 0, 0, 1},
            {0, 0, 0, 0, 1},
            {0, 0, 0, 0, 1}
        });

        FONT_MAP.put('5', new int[][]{
            {1, 1, 1, 1, 1},
            {1, 0, 0, 0, 0},
            {1, 1, 1, 1, 0},
            {0, 0, 0, 0, 1},
            {0, 0, 0, 0, 1},
            {1, 0, 0, 0, 1},
            {0, 1, 1, 1, 0}
        });

        FONT_MAP.put('6', new int[][]{
            {0, 1, 1, 1, 0},
            {1, 0, 0, 0, 0},
            {1, 0, 0, 0, 0},
            {1, 1, 1, 1, 0},
            {1, 0, 0, 0, 1},
            {1, 0, 0, 0, 1},
            {0, 1, 1, 1, 0}
        });

        FONT_MAP.put('7', new int[][]{
            {1, 1, 1, 1, 1},
            {0, 0, 0, 0, 1},
            {0, 0, 0, 1, 0},
            {0, 0, 1, 0, 0},
            {0, 0, 1, 0, 0},
            {0, 0, 1, 0, 0},
            {0, 0, 1, 0, 0}
        });

        FONT_MAP.put('8', new int[][]{
            {0, 1, 1, 1, 0},
            {1, 0, 0, 0, 1},
            {1, 0, 0, 0, 1},
            {0, 1, 1, 1, 0},
            {1, 0, 0, 0, 1},
            {1, 0, 0, 0, 1},
            {0, 1, 1, 1, 0}
        });

        FONT_MAP.put('9', new int[][]{
            {0, 1, 1, 1, 0},
            {1, 0, 0, 0, 1},
            {1, 0, 0, 0, 1},
            {0, 1, 1, 1, 1},
            {0, 0, 0, 0, 1},
            {0, 0, 0, 0, 1},
            {0, 1, 1, 1, 0}
        });

        FONT_MAP.put('%', new int[][]{
            {1, 1, 0, 0, 1},
            {1, 1, 0, 1, 0},
            {0, 0, 1, 0, 0},
            {0, 1, 0, 0, 0},
            {1, 0, 0, 1, 1},
            {0, 0, 0, 1, 1},
            {0, 0, 0, 0, 0}
        });

        FONT_MAP.put(':', new int[][]{
            {0, 0, 0, 0, 0},
            {0, 0, 1, 0, 0},
            {0, 0, 1, 0, 0},
            {0, 0, 0, 0, 0},
            {0, 0, 1, 0, 0},
            {0, 0, 1, 0, 0},
            {0, 0, 0, 0, 0}
        });

        FONT_MAP.put('.', new int[][]{
            {0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0},
            {0, 0, 1, 0, 0},
            {0, 0, 1, 0, 0}
        });

        FONT_MAP.put('/', new int[][]{
            {0, 0, 0, 0, 1},
            {0, 0, 0, 1, 0},
            {0, 0, 0, 1, 0},
            {0, 0, 1, 0, 0},
            {0, 1, 0, 0, 0},
            {0, 1, 0, 0, 0},
            {1, 0, 0, 0, 0}
        });

        FONT_MAP.put('+', new int[][]{
            {0, 0, 0, 0, 0},
            {0, 0, 1, 0, 0},
            {0, 0, 1, 0, 0},
            {1, 1, 1, 1, 1},
            {0, 0, 1, 0, 0},
            {0, 0, 1, 0, 0},
            {0, 0, 0, 0, 0}
        });

        FONT_MAP.put('-', new int[][]{
            {0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0},
            {1, 1, 1, 1, 1},
            {0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0}
        });

        FONT_MAP.put('s', new int[][]{
            {0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0},
            {0, 1, 1, 1, 0},
            {1, 0, 0, 0, 0},
            {0, 1, 1, 0, 0},
            {0, 0, 0, 1, 0},
            {1, 1, 1, 0, 0}
        });

        FONT_MAP.put('x', new int[][]{
            {0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0},
            {1, 0, 0, 0, 1},
            {0, 1, 0, 1, 0},
            {0, 0, 1, 0, 0},
            {0, 1, 0, 1, 0},
            {1, 0, 0, 0, 1}
        });

        FONT_MAP.put('A', new int[][]{
            {0, 1, 1, 1, 0},
            {1, 0, 0, 0, 1},
            {1, 0, 0, 0, 1},
            {1, 1, 1, 1, 1},
            {1, 0, 0, 0, 1},
            {1, 0, 0, 0, 1},
            {1, 0, 0, 0, 1}
        });

        FONT_MAP.put('B', new int[][]{
            {1, 1, 1, 1, 0},
            {1, 0, 0, 0, 1},
            {1, 0, 0, 0, 1},
            {1, 1, 1, 1, 0},
            {1, 0, 0, 0, 1},
            {1, 0, 0, 0, 1},
            {1, 1, 1, 1, 0}
        });

        FONT_MAP.put('C', new int[][]{
            {0, 1, 1, 1, 0},
            {1, 0, 0, 0, 1},
            {1, 0, 0, 0, 0},
            {1, 0, 0, 0, 0},
            {1, 0, 0, 0, 0},
            {1, 0, 0, 0, 1},
            {0, 1, 1, 1, 0}
        });

        FONT_MAP.put('D', new int[][]{
            {1, 1, 1, 1, 0},
            {1, 0, 0, 0, 1},
            {1, 0, 0, 0, 1},
            {1, 0, 0, 0, 1},
            {1, 0, 0, 0, 1},
            {1, 0, 0, 0, 1},
            {1, 1, 1, 1, 0}
        });

        FONT_MAP.put('E', new int[][]{
            {1, 1, 1, 1, 1},
            {1, 0, 0, 0, 0},
            {1, 0, 0, 0, 0},
            {1, 1, 1, 1, 0},
            {1, 0, 0, 0, 0},
            {1, 0, 0, 0, 0},
            {1, 1, 1, 1, 1}
        });

        FONT_MAP.put('F', new int[][]{
            {1, 1, 1, 1, 1},
            {1, 0, 0, 0, 0},
            {1, 0, 0, 0, 0},
            {1, 1, 1, 1, 0},
            {1, 0, 0, 0, 0},
            {1, 0, 0, 0, 0},
            {1, 0, 0, 0, 0}
        });

        FONT_MAP.put('I', new int[][]{
            {1, 1, 1, 1, 1},
            {0, 0, 1, 0, 0},
            {0, 0, 1, 0, 0},
            {0, 0, 1, 0, 0},
            {0, 0, 1, 0, 0},
            {0, 0, 1, 0, 0},
            {1, 1, 1, 1, 1}
        });

        FONT_MAP.put('K', new int[][]{
            {1, 0, 0, 0, 1},
            {1, 0, 0, 1, 0},
            {1, 0, 1, 0, 0},
            {1, 1, 0, 0, 0},
            {1, 0, 1, 0, 0},
            {1, 0, 0, 1, 0},
            {1, 0, 0, 0, 1}
        });

        FONT_MAP.put('L', new int[][]{
            {1, 0, 0, 0, 0},
            {1, 0, 0, 0, 0},
            {1, 0, 0, 0, 0},
            {1, 0, 0, 0, 0},
            {1, 0, 0, 0, 0},
            {1, 0, 0, 0, 0},
            {1, 1, 1, 1, 1}
        });

        FONT_MAP.put('M', new int[][]{
            {1, 0, 0, 0, 1},
            {1, 1, 0, 1, 1},
            {1, 0, 1, 0, 1},
            {1, 0, 0, 0, 1},
            {1, 0, 0, 0, 1},
            {1, 0, 0, 0, 1},
            {1, 0, 0, 0, 1}
        });

        FONT_MAP.put('N', new int[][]{
            {1, 0, 0, 0, 1},
            {1, 1, 0, 0, 1},
            {1, 0, 1, 0, 1},
            {1, 0, 0, 1, 1},
            {1, 0, 0, 0, 1},
            {1, 0, 0, 0, 1},
            {1, 0, 0, 0, 1}
        });

        FONT_MAP.put('O', new int[][]{
            {0, 1, 1, 1, 0},
            {1, 0, 0, 0, 1},
            {1, 0, 0, 0, 1},
            {1, 0, 0, 0, 1},
            {1, 0, 0, 0, 1},
            {1, 0, 0, 0, 1},
            {0, 1, 1, 1, 0}
        });

        FONT_MAP.put('P', new int[][]{
            {1, 1, 1, 1, 0},
            {1, 0, 0, 0, 1},
            {1, 0, 0, 0, 1},
            {1, 1, 1, 1, 0},
            {1, 0, 0, 0, 0},
            {1, 0, 0, 0, 0},
            {1, 0, 0, 0, 0}
        });

        FONT_MAP.put('R', new int[][]{
            {1, 1, 1, 1, 0},
            {1, 0, 0, 0, 1},
            {1, 0, 0, 0, 1},
            {1, 1, 1, 1, 0},
            {1, 0, 1, 0, 0},
            {1, 0, 0, 1, 0},
            {1, 0, 0, 0, 1}
        });

        FONT_MAP.put('S', new int[][]{
            {0, 1, 1, 1, 1},
            {1, 0, 0, 0, 0},
            {1, 0, 0, 0, 0},
            {0, 1, 1, 1, 0},
            {0, 0, 0, 0, 1},
            {0, 0, 0, 0, 1},
            {1, 1, 1, 1, 0}
        });

        FONT_MAP.put('T', new int[][]{
            {1, 1, 1, 1, 1},
            {0, 0, 1, 0, 0},
            {0, 0, 1, 0, 0},
            {0, 0, 1, 0, 0},
            {0, 0, 1, 0, 0},
            {0, 0, 1, 0, 0},
            {0, 0, 1, 0, 0}
        });

        FONT_MAP.put('U', new int[][]{
            {1, 0, 0, 0, 1},
            {1, 0, 0, 0, 1},
            {1, 0, 0, 0, 1},
            {1, 0, 0, 0, 1},
            {1, 0, 0, 0, 1},
            {1, 0, 0, 0, 1},
            {0, 1, 1, 1, 0}
        });

        FONT_MAP.put('Z', new int[][]{
            {1, 1, 1, 1, 1},
            {0, 0, 0, 0, 1},
            {0, 0, 0, 1, 0},
            {0, 0, 1, 0, 0},
            {0, 1, 0, 0, 0},
            {1, 0, 0, 0, 0},
            {1, 1, 1, 1, 1}
        });

        FONT_MAP.put(' ', new int[][]{
            {0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0}
        });
    }

    public DotMatrixTextView(Context context) {
        super(context);
        init();
    }

    public DotMatrixTextView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DotMatrixTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        activePaint.setStyle(Paint.Style.FILL);
        activePaint.setColor(textColor);
        inactivePaint.setStyle(Paint.Style.FILL);
        inactivePaint.setColor(inactiveDotColor);
    }

    public void setText(String text) {
        this.text = text != null ? text : "";
        requestLayout();
        invalidate();
    }

    public void setTextColor(int color) {
        this.textColor = color;
        activePaint.setColor(color);
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int charCount = Math.max(1, text.length());
        int desiredHeight = (int) (18 * getResources().getDisplayMetrics().density);
        if (heightMode == MeasureSpec.EXACTLY || (heightMode == MeasureSpec.AT_MOST && heightSize > 0)) {
            desiredHeight = heightSize;
        }

        float dotSpacing = desiredHeight / (float) FONT_HEIGHT;
        int desiredWidth = (int) (charCount * (FONT_WIDTH + 1) * dotSpacing);

        int width = resolveSize(desiredWidth, widthMeasureSpec);
        int height = resolveSize(desiredHeight, heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (text == null || text.isEmpty()) return;

        int width = getWidth();
        int height = getHeight();
        if (width == 0 || height == 0) return;

        int charCount = text.length();
        float dotSpacing = Math.min((float) height / FONT_HEIGHT, (float) width / (charCount * (FONT_WIDTH + 1)));
        float dotDiameter = dotSpacing * 0.72f;
        float dotRadius = dotDiameter / 2f;

        float totalTextWidth = charCount * (FONT_WIDTH + 1) * dotSpacing - dotSpacing;
        float startX = (width - totalTextWidth) / 2f + dotSpacing / 2f;
        float startY = (height - FONT_HEIGHT * dotSpacing) / 2f + dotSpacing / 2f;

        for (int c = 0; c < charCount; c++) {
            char ch = text.charAt(c);
            int[][] pattern = FONT_MAP.get(ch);

            float charOffsetX = startX + c * (FONT_WIDTH + 1) * dotSpacing;

            if (pattern != null) {
                for (int row = 0; row < FONT_HEIGHT; row++) {
                    for (int col = 0; col < FONT_WIDTH; col++) {
                        float cx = charOffsetX + col * dotSpacing;
                        float cy = startY + row * dotSpacing;

                        boolean isActive = pattern[row][col] == 1;
                        canvas.drawCircle(cx, cy, dotRadius, isActive ? activePaint : inactivePaint);
                    }
                }
            }
        }
    }
}
