package com.nothing.player;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Authentic Nothing OS NDOT 3x3 Dot-Matrix Pattern Lock View.
 * Renders 9 discrete micro-LED dots with Nothing Red lines and tactile haptic feedback.
 */
public class DotMatrixPatternView extends View {

    public interface OnPatternListener {
        void onPatternComplete(String pattern);
        void onPatternStart();
    }

    private static final int GRID_SIZE = 3;
    private final Node[] nodes = new Node[9];
    private final List<Integer> selectedNodes = new ArrayList<>();
    private final Set<Integer> selectedSet = new HashSet<>();

    private Paint ringPaint;
    private Paint dotPaint;
    private Paint activeRingPaint;
    private Paint activeDotPaint;
    private Paint linePaint;
    private Paint errorLinePaint;
    private Paint errorRingPaint;

    private float currentTouchX = -1;
    private float currentTouchY = -1;
    private boolean isTouching = false;
    private boolean isError = false;
    private boolean isEnabled = true;

    private float hitRadius;
    private float ringRadius;
    private float dotRadius;

    private OnPatternListener listener;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Vibrator vibrator;

    private static class Node {
        final int id;
        float x, y;
        Node(int id) {
            this.id = id;
        }
    }

    public DotMatrixPatternView(Context context) {
        super(context);
        init(context);
    }

    public DotMatrixPatternView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public DotMatrixPatternView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        float density = getResources().getDisplayMetrics().density;

        hitRadius = 38 * density;
        ringRadius = 22 * density;
        dotRadius = 5.5f * density;

        // Inactive LED Ring
        ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ringPaint.setColor(Color.parseColor("#26FFFFFF")); // Faint discrete LED
        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(2 * density);

        // Inactive Center Micro Dot
        dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotPaint.setColor(Color.parseColor("#4DFFFFFF"));
        dotPaint.setStyle(Paint.Style.FILL);

        // Active Nothing Red LED Ring
        activeRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        activeRingPaint.setColor(Color.parseColor("#D71921")); // Nothing Signature Red
        activeRingPaint.setStyle(Paint.Style.STROKE);
        activeRingPaint.setStrokeWidth(2.8f * density);

        // Active Nothing Red Center Dot
        activeDotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        activeDotPaint.setColor(Color.parseColor("#D71921"));
        activeDotPaint.setStyle(Paint.Style.FILL);

        // Connecting Line Paint
        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(Color.parseColor("#D71921"));
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(4.5f * density);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);

        // Error Line Paint
        errorLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        errorLinePaint.setColor(Color.parseColor("#FF2A33"));
        errorLinePaint.setStyle(Paint.Style.STROKE);
        errorLinePaint.setStrokeWidth(4.5f * density);
        errorLinePaint.setStrokeCap(Paint.Cap.ROUND);
        errorLinePaint.setStrokeJoin(Paint.Join.ROUND);

        errorRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        errorRingPaint.setColor(Color.parseColor("#FF2A33"));
        errorRingPaint.setStyle(Paint.Style.STROKE);
        errorRingPaint.setStrokeWidth(3 * density);

        for (int i = 0; i < 9; i++) {
            nodes[i] = new Node(i);
        }
    }

    public void setOnPatternListener(OnPatternListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        int size = Math.min(width, height);
        if (size <= 0) size = (int) (300 * getResources().getDisplayMetrics().density);
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float padding = 40 * getResources().getDisplayMetrics().density;
        float usableSize = Math.min(w, h) - (padding * 2);
        float step = usableSize / (GRID_SIZE - 1);
        float startX = (w - usableSize) / 2f;
        float startY = (h - usableSize) / 2f;

        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                int id = row * GRID_SIZE + col;
                nodes[id].x = startX + col * step;
                nodes[id].y = startY + row * step;
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled) return false;

        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                handler.removeCallbacksAndMessages(null);
                resetInternal();
                isError = false;
                int hit = findHitNode(x, y);
                if (hit != -1) {
                    addNode(hit);
                    isTouching = true;
                    currentTouchX = x;
                    currentTouchY = y;
                    if (listener != null) listener.onPatternStart();
                }
                invalidate();
                return true;

            case MotionEvent.ACTION_MOVE:
                if (!isTouching) return false;
                currentTouchX = x;
                currentTouchY = y;

                int moveHit = findHitNode(x, y);
                if (moveHit != -1 && !selectedSet.contains(moveHit)) {
                    // Check if there is an in-between node skipped
                    if (!selectedNodes.isEmpty()) {
                        int lastNode = selectedNodes.get(selectedNodes.size() - 1);
                        int bridge = getBridgeNode(lastNode, moveHit);
                        if (bridge != -1 && !selectedSet.contains(bridge)) {
                            addNode(bridge);
                        }
                    }
                    addNode(moveHit);
                }
                invalidate();
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (!isTouching) return false;
                isTouching = false;
                currentTouchX = -1;
                currentTouchY = -1;
                invalidate();

                if (!selectedNodes.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    for (int n : selectedNodes) {
                        sb.append(n);
                    }
                    if (listener != null) {
                        listener.onPatternComplete(sb.toString());
                    }
                }
                return true;
        }
        return super.onTouchEvent(event);
    }

    private void addNode(int id) {
        selectedNodes.add(id);
        selectedSet.add(id);
        performTactileTick();
    }

    private int findHitNode(float x, float y) {
        for (Node node : nodes) {
            float dx = node.x - x;
            float dy = node.y - y;
            if ((dx * dx + dy * dy) <= (hitRadius * hitRadius)) {
                return node.id;
            }
        }
        return -1;
    }

    private int getBridgeNode(int n1, int n2) {
        // Horizontal bridges: 0-2 (1), 3-5 (4), 6-8 (7)
        if ((n1 == 0 && n2 == 2) || (n1 == 2 && n2 == 0)) return 1;
        if ((n1 == 3 && n2 == 5) || (n1 == 5 && n2 == 3)) return 4;
        if ((n1 == 6 && n2 == 8) || (n1 == 8 && n2 == 6)) return 7;

        // Vertical bridges: 0-6 (3), 1-7 (4), 2-8 (5)
        if ((n1 == 0 && n2 == 6) || (n1 == 6 && n2 == 0)) return 3;
        if ((n1 == 1 && n2 == 7) || (n1 == 7 && n2 == 1)) return 4;
        if ((n1 == 2 && n2 == 8) || (n1 == 8 && n2 == 2)) return 5;

        // Diagonal bridges: 0-8 (4), 2-6 (4)
        if ((n1 == 0 && n2 == 8) || (n1 == 8 && n2 == 0)) return 4;
        if ((n1 == 2 && n2 == 6) || (n1 == 6 && n2 == 2)) return 4;

        return -1;
    }

    private void performTactileTick() {
        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        try {
            if (vibrator != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(18, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(18);
                }
            }
        } catch (Exception ignored) {}
    }

    public void showError() {
        isError = true;
        try {
            if (vibrator != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(120);
                }
            }
        } catch (Exception ignored) {}
        invalidate();
        handler.postDelayed(this::reset, 700);
    }

    public void reset() {
        resetInternal();
        isError = false;
        invalidate();
    }

    private void resetInternal() {
        selectedNodes.clear();
        selectedSet.clear();
        currentTouchX = -1;
        currentTouchY = -1;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        Paint curLinePaint = isError ? errorLinePaint : linePaint;
        Paint curActiveRing = isError ? errorRingPaint : activeRingPaint;

        // 1. Draw Connecting Lines between Selected Nodes
        if (selectedNodes.size() > 1) {
            Path path = new Path();
            Node first = nodes[selectedNodes.get(0)];
            path.moveTo(first.x, first.y);
            for (int i = 1; i < selectedNodes.size(); i++) {
                Node next = nodes[selectedNodes.get(i)];
                path.lineTo(next.x, next.y);
            }
            canvas.drawPath(path, curLinePaint);
        }

        // 2. Draw live line to finger position while dragging
        if (isTouching && !selectedNodes.isEmpty() && currentTouchX >= 0 && currentTouchY >= 0) {
            Node last = nodes[selectedNodes.get(selectedNodes.size() - 1)];
            canvas.drawLine(last.x, last.y, currentTouchX, currentTouchY, curLinePaint);
        }

        // 3. Draw All 9 Discrete Dot-Matrix LED Nodes
        for (Node node : nodes) {
            boolean isSelected = selectedSet.contains(node.id);

            if (isSelected) {
                // Outer Red LED Ring
                canvas.drawCircle(node.x, node.y, ringRadius, curActiveRing);
                // Center Solid Red Dot
                canvas.drawCircle(node.x, node.y, dotRadius * 1.25f, activeDotPaint);
            } else {
                // Inactive Discrete LED Ring
                canvas.drawCircle(node.x, node.y, ringRadius, ringPaint);
                // Inactive Center Dot
                canvas.drawCircle(node.x, node.y, dotRadius, dotPaint);
            }
        }
    }
}

