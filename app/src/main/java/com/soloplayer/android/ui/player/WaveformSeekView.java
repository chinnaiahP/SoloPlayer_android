package com.soloplayer.android.ui.player;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

/** Seek bar styled as a waveform scrubber: fixed pseudo-random bars (seeded per
 * track so the shape is stable), played portion lit in purple, glowing thumb
 * dot at the play position. Exposes the same 0-1000 progress scale the old
 * SeekBar used so callers can stay unchanged. */
public final class WaveformSeekView extends View {

    public interface OnSeekListener {
        void onStartTracking(WaveformSeekView view);
        void onProgressChanged(WaveformSeekView view, int progress);
        void onStopTracking(WaveformSeekView view, int progress);
    }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int purple;
    private int lilac;
    private int progress; // 0..1000
    private long seed;
    private float[] bars = new float[0];
    private boolean tracking;
    private OnSeekListener listener;

    public WaveformSeekView(Context context, int purple, int lilac) {
        super(context);
        this.purple = purple;
        this.lilac = lilac;
    }

    public void setOnSeekListener(OnSeekListener l) { this.listener = l; }

    public void setProgress(int value) {
        this.progress = Math.max(0, Math.min(1000, value));
        invalidate();
    }

    public int getProgress() { return this.progress; }

    /** Different seed -> different stable waveform, so each track looks unique. */
    public void setSeed(String key) {
        long s = key == null ? 0 : key.hashCode();
        if (s == this.seed) return;
        this.seed = s;
        this.bars = new float[0];
        invalidate();
    }

    private float barHeight(int index) {
        if (index >= this.bars.length) {
            float[] bigger = new float[index + 64];
            System.arraycopy(this.bars, 0, bigger, 0, this.bars.length);
            java.util.Random r = new java.util.Random(this.seed == 0 ? 1 : this.seed);
            for (int i = 0; i < bigger.length; i++) {
                if (i < this.bars.length) bigger[i] = this.bars[i];
                else {
                    // Smooth-ish wave: neighboring bars correlate, amplitude varies.
                    double wave = 0.30 + 0.22 * Math.sin(i * 0.55 + this.seed % 17)
                            + 0.18 * Math.sin(i * 1.7 + this.seed % 7)
                            + r.nextFloat() * 0.30;
                    bigger[i] = (float) Math.max(0.08, Math.min(1.0, wave));
                }
            }
            this.bars = bigger;
        }
        return this.bars[index];
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        float gap = dp(3), barW = dp(2.4f);
        int count = Math.max(8, (int) ((w + gap) / (barW + gap)));
        float totalW = count * barW + (count - 1) * gap;
        float left = (w - totalW) * 0.5f;
        float center = h * 0.5f;
        float maxBar = h * 0.86f;
        int played = Math.round(count * (this.progress / 1000.0f));
        for (int i = 0; i < count; i++) {
            float bh = Math.max(dp(2.5f), barHeight(i) * maxBar);
            float x = left + i * (barW + gap);
            boolean on = i < played;
            paint.setColor(i % 4 == 0 ? lilac : purple);
            paint.setAlpha(on ? 235 : 90);
            canvas.drawRoundRect(new RectF(x, center - bh * 0.5f, x + barW, center + bh * 0.5f), barW, barW, paint);
        }
        // glowing thumb dot at the play position
        float tx = left + played * (barW + gap) - gap * 0.5f;
        paint.setColor(lilac);
        paint.setAlpha(70);
        canvas.drawCircle(tx, center, dp(7), paint);
        paint.setAlpha(255);
        canvas.drawCircle(tx, center, dp(3.4f), paint);
    }

    private void touch(MotionEvent e) {
        float w = getWidth();
        int p = w <= 0 ? 0 : Math.round(Math.max(0, Math.min(w, e.getX())) / w * 1000.0f);
        if (p != this.progress) {
            this.progress = p;
            invalidate();
            if (this.listener != null) this.listener.onProgressChanged(this, p);
        }
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                getParent().requestDisallowInterceptTouchEvent(true);
                this.tracking = true;
                if (this.listener != null) this.listener.onStartTracking(this);
                touch(e);
                return true;
            case MotionEvent.ACTION_MOVE:
                if (this.tracking) touch(e);
                return true;
            case MotionEvent.ACTION_UP:
                if (this.tracking) {
                    this.tracking = false;
                    touch(e);
                    if (this.listener != null) this.listener.onStopTracking(this, this.progress);
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
                this.tracking = false;
                return true;
        }
        return super.onTouchEvent(e);
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }
}
