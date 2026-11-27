package com.nothing.player;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.audiofx.Visualizer;
import android.util.AttributeSet;
import android.view.View;
import androidx.core.content.ContextCompat;

/**
 * Nothing OS Dot-Matrix Spectrum Visualizer.
 * Uses Android AudioEffect Visualizer FFT when RECORD_AUDIO is permitted and attaches cleanly,
 * with a zero-permission rhythm-reactive LED matrix fallback so playback never requires mic prompts.
 */
public class DotMatrixSpectrumView extends View {
    private static final int BAR_COUNT = 18;
    private static final int ROWS_PER_BAR = 7;

    private final Paint dimDotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint activeWhitePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint peakRedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final float[] levels = new float[BAR_COUNT];
    private final int[] peaks = new int[BAR_COUNT];
    private Visualizer visualizer;
    private int boundSessionId = -1;
    private boolean playing = false;
    private boolean hasRealFft = false;
    private long phaseTick = 0L;

    private final Runnable pulseTicker = new Runnable() {
        @Override
        public void run() {
            if (!isAttachedToWindow()) return;
            if (!hasRealFft) {
                stepReactiveFallback();
                invalidate();
            }
            if (playing && getVisibility() == VISIBLE) {
                postDelayed(this, 60L);
            }
        }
    };

    public DotMatrixSpectrumView(Context context) {
        super(context);
        init();
    }

    public DotMatrixSpectrumView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DotMatrixSpectrumView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        dimDotPaint.setColor(Color.parseColor("#26FFFFFF"));
        activeWhitePaint.setColor(Color.WHITE);
        peakRedPaint.setColor(Color.parseColor("#D71921"));
    }

    public void attachSession(int audioSessionId, boolean isPlaying) {
        this.playing = isPlaying;
        if (audioSessionId > 0 && audioSessionId != boundSessionId) {
            releaseVisualizer();
            boundSessionId = audioSessionId;
            tryAttachVisualizer(audioSessionId);
        }
        updateAnimationState();
    }

    public void setPlaying(boolean isPlaying) {
        this.playing = isPlaying;
        if (visualizer != null) {
            try {
                visualizer.setEnabled(isPlaying);
            } catch (Throwable ignored) {}
        }
        updateAnimationState();
    }

    private void tryAttachVisualizer(int sessionId) {
        hasRealFft = false;
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        try {
            Visualizer v = new Visualizer(sessionId);
            int[] range = Visualizer.getCaptureSizeRange();
            v.setCaptureSize(Math.min(range[1], 256));
            v.setDataCaptureListener(new Visualizer.OnDataCaptureListener() {
                @Override
                public void onWaveFormDataCapture(Visualizer vis, byte[] waveform, int samplingRate) {}

                @Override
                public void onFftDataCapture(Visualizer vis, byte[] fft, int samplingRate) {
                    if (fft == null || fft.length < 4) return;
                    hasRealFft = true;
                    int bins = Math.min(fft.length / 2, 48);
                    for (int bar = 0; bar < BAR_COUNT; bar++) {
                        int idx = 1 + (bar * Math.max(1, bins - 2)) / BAR_COUNT;
                        if (idx * 2 + 1 >= fft.length) idx = (fft.length - 2) / 2;
                        float re = fft[idx * 2];
                        float im = fft[idx * 2 + 1];
                        float mag = (float) Math.hypot(re, im);
                        float norm = Math.min(1.0f, (float) (Math.log1p(mag) / 4.4));
                        levels[bar] = Math.max(norm, levels[bar] * 0.72f);
                    }
                    postInvalidateOnAnimation();
                }
            }, Visualizer.getMaxCaptureRate() / 2, false, true);
            v.setEnabled(playing);
            visualizer = v;
        } catch (Throwable ignored) {
            hasRealFft = false;
            releaseVisualizer();
        }
    }

    private void stepReactiveFallback() {
        phaseTick++;
        for (int i = 0; i < BAR_COUNT; i++) {
            if (!playing) {
                levels[i] *= 0.65f;
                peaks[i] = Math.max(0, peaks[i] - 1);
            } else {
                double wave1 = Math.abs(Math.sin(phaseTick * 0.34 + i * 0.55));
                double wave2 = Math.abs(Math.cos(phaseTick * 0.21 - i * 0.31));
                float target = (float) Math.min(1.0, 0.18 + 0.75 * (wave1 * 0.65 + wave2 * 0.35));
                levels[i] = levels[i] * 0.45f + target * 0.55f;
            }
        }
    }

    private void updateAnimationState() {
        removeCallbacks(pulseTicker);
        if (playing && !hasRealFft && isAttachedToWindow()) {
            post(pulseTicker);
        } else {
            invalidate();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        updateAnimationState();
    }

    @Override
    protected void onDetachedFromWindow() {
        removeCallbacks(pulseTicker);
        releaseVisualizer();
        super.onDetachedFromWindow();
    }

    public void releaseVisualizer() {
        if (visualizer != null) {
            try {
                visualizer.setEnabled(false);
            } catch (Throwable ignored) {}
            try {
                visualizer.release();
            } catch (Throwable ignored) {}
            visualizer = null;
        }
        hasRealFft = false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth() - getPaddingLeft() - getPaddingRight();
        int h = getHeight() - getPaddingTop() - getPaddingBottom();
        if (w <= 0 || h <= 0) return;

        float colStep = w / (float) BAR_COUNT;
        float rowStep = h / (float) ROWS_PER_BAR;
        float radius = Math.max(1.8f, Math.min(colStep, rowStep) * 0.32f);

        for (int c = 0; c < BAR_COUNT; c++) {
            float cx = getPaddingLeft() + colStep * (c + 0.5f);
            int litCount = Math.round(Math.max(0f, Math.min(1f, levels[c])) * ROWS_PER_BAR);
            if (!playing && litCount == 0) {
                // Keep subtle baseline LED row lit even when paused
                litCount = 0;
            }
            if (litCount > peaks[c]) {
                peaks[c] = litCount;
            } else if (peaks[c] > 0 && phaseTick % 3 == 0) {
                peaks[c]--;
            }

            for (int r = 0; r < ROWS_PER_BAR; r++) {
                int rowFromBottom = ROWS_PER_BAR - 1 - r;
                float cy = getPaddingTop() + rowStep * (r + 0.5f);
                Paint p;
                if (litCount > 0 && rowFromBottom == litCount - 1 && litCount >= ROWS_PER_BAR - 1) {
                    p = peakRedPaint;
                } else if (peaks[c] > 0 && rowFromBottom == peaks[c] - 1 && litCount > 0) {
                    p = peakRedPaint;
                } else if (rowFromBottom < litCount) {
                    p = activeWhitePaint;
                } else if (rowFromBottom == 0) {
                    p = playing ? peakRedPaint : dimDotPaint;
                } else {
                    p = dimDotPaint;
                }
                canvas.drawCircle(cx, cy, radius, p);
            }
        }
    }
}
