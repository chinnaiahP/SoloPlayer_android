package com.soloplayer.android.ui.player;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

/** Lightweight playback-state animation drawn around the existing album artwork. */
public final class AudioArtworkEffectView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int purple;
    private final int lilac;
    private boolean playing;
    private boolean showBars = true;

    public AudioArtworkEffectView(Context context, int purple, int lilac) {
        super(context);
        this.purple = purple;
        this.lilac = lilac;
        setClickable(false);
        setFocusable(false);
    }

    public void setPlaying(boolean value) {
        if (this.playing == value) return;
        this.playing = value;
        invalidate();
    }

    public void setShowBars(boolean value) {
        if (this.showBars == value) return;
        this.showBars = value;
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width = getWidth(), height = getHeight();
        if (width <= 0 || height <= 0) return;
        float phase = playing ? android.os.SystemClock.uptimeMillis() / 1000.0f : 0.0f;
        float cx = width * 0.5f, cy = height * (showBars ? 0.405f : 0.5f);
        float base = Math.min(width * 0.35f, height * 0.325f);
        for (int layer = 0; layer < 3; layer++) {
            Path wave = new Path();
            int samples = 144;
            for (int i = 0; i <= samples; i++) {
                float angle = (float) (Math.PI * 2.0 * i / samples);
                float pulse = playing
                        ? (float) (Math.sin(angle * 5.0f + phase * 2.1f) * 0.045f
                        + Math.sin(angle * 9.0f - phase * 1.35f) * 0.022f)
                        : (float) (Math.sin(angle * 5.0f) * 0.012f);
                float radius = base + layer * dp(6) + pulse * base * (layer == 0 ? 1.5f : 0.75f);
                float x = cx + (float) Math.cos(angle) * radius;
                float y = cy + (float) Math.sin(angle) * radius;
                if (i == 0) wave.moveTo(x, y); else wave.lineTo(x, y);
            }
            wave.close();
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setColor(layer == 2 ? lilac : purple);
            paint.setStrokeWidth(dp(layer == 0 ? 7 : (layer == 1 ? 3 : 1.5f)));
            paint.setAlpha(layer == 0 ? (playing ? 24 : 12)
                    : (playing ? (layer == 1 ? 72 : 220) : (layer == 1 ? 38 : 145)));
            canvas.drawPath(wave, paint);
        }
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(lilac);
        paint.setAlpha(playing ? 235 : 125);
        float dotAngle = (float) (phase * 0.75f - Math.PI * 0.65);
        float dotRadius = base + dp(6);
        canvas.drawCircle(cx + (float) Math.cos(dotAngle) * dotRadius,
                cy + (float) Math.sin(dotAngle) * dotRadius, dp(3.5f), paint);

        if (showBars) {
        int bars = 17;
        float gap = dp(3), available = width * 0.78f;
        float barWidth = (available - gap * (bars - 1)) / bars;
        float left = (width - available) * 0.5f, baseline = height * 0.95f;
        for (int i = 0; i < bars; i++) {
            float energy = playing ? 0.5f + 0.5f * (float) Math.sin(phase * 5.1f + i * 0.86f)
                    : 0.12f + 0.11f * (float) Math.sin(i * 0.75f);
            float envelope = (float) (0.45 + 0.55 * Math.sin(Math.PI * (i + 1) / (bars + 1)));
            float barHeight = dp(5) + dp(24) * Math.max(0.06f, energy) * envelope;
            float x = left + i * (barWidth + gap);
            paint.setColor(purple);
            paint.setAlpha(playing ? 62 : 20);
            canvas.drawRoundRect(new RectF(x - dp(1), baseline - barHeight - dp(2),
                    x + barWidth + dp(1), baseline + dp(1)), dp(3), dp(3), paint);
            paint.setColor(i % 3 == 0 ? lilac : purple);
            paint.setAlpha(playing ? 220 : 82);
            canvas.drawRoundRect(new RectF(x, baseline - barHeight, x + barWidth, baseline),
                    dp(2), dp(2), paint);
        }
        }
        if (playing) postInvalidateDelayed(48L);
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
