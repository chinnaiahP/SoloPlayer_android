package com.soloplayer.android.ui.player;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

/** Ambient purple wave visualization that fills the player card background —
 * layered sine ribbons plus drifting particles, animated while playing. */
public final class AudioWaveBackdropView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int purple;
    private final int lilac;
    private boolean playing;
    private final float[] dotX = new float[26];
    private final float[] dotY = new float[26];
    private final float[] dotR = new float[26];
    private final float[] dotSpeed = new float[26];
    private boolean dotsSeeded;

    public AudioWaveBackdropView(Context context, int purple, int lilac) {
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

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width = getWidth(), height = getHeight();
        if (width <= 0 || height <= 0) return;
        if (!dotsSeeded) {
            java.util.Random random = new java.util.Random(7L);
            for (int i = 0; i < dotX.length; i++) {
                dotX[i] = random.nextFloat();
                dotY[i] = random.nextFloat();
                dotR[i] = 0.8f + random.nextFloat() * 1.6f;
                dotSpeed[i] = 0.02f + random.nextFloat() * 0.05f;
            }
            dotsSeeded = true;
        }
        float phase = playing ? android.os.SystemClock.uptimeMillis() / 1000.0f : 0.0f;
        float cy = height * 0.44f;

        for (int layer = 0; layer < 3; layer++) {
            Path wave = new Path();
            int samples = 160;
            for (int i = 0; i <= samples; i++) {
                float t = (float) i / samples;
                float x = t * width;
                float envelope = (float) Math.sin(Math.PI * t); // fade both edges
                float amp = (dp(14) + layer * dp(8)) * envelope;
                float y = cy
                        + (float) Math.sin(t * Math.PI * 4.4 + phase * (1.5 + layer * 0.45) + layer * 1.9) * amp
                        + (float) Math.sin(t * Math.PI * 9.0 - phase * (0.9 + layer * 0.3)) * amp * 0.3f
                        + (layer - 1) * dp(9);
                if (i == 0) wave.moveTo(x, y); else wave.lineTo(x, y);
            }
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setColor(layer == 1 ? lilac : purple);
            paint.setStrokeWidth(dp(layer == 1 ? 1.2f : 1.8f));
            paint.setAlpha(playing ? (layer == 1 ? 150 : 90) : (layer == 1 ? 95 : 55));
            canvas.drawPath(wave, paint);
        }

        paint.setStyle(Paint.Style.FILL);
        for (int i = 0; i < dotX.length; i++) {
            float drift = playing ? (phase * dotSpeed[i]) % 1.0f : 0.0f;
            float x = ((dotX[i] + drift) % 1.0f) * width;
            float y = cy + (dotY[i] - 0.5f) * height * 0.34f
                    + (float) Math.sin(phase * 1.4 + i * 1.3) * dp(5);
            paint.setColor(i % 4 == 0 ? lilac : purple);
            paint.setAlpha(playing ? 120 : 70);
            canvas.drawCircle(x, y, dp(dotR[i]), paint);
        }
        if (playing) postInvalidateDelayed(50L);
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
