package com.soloplayer.android.online;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Tiny async artwork loader for remote thumbnails. Memory-cached; the target
 * view is tagged with the requested URL so recycled/stale views never show
 * the wrong image.
 */
public final class OnlineArtwork {
    private static final LruCache<String, Bitmap> CACHE = new LruCache<String, Bitmap>(24 * 1024 * 1024) {
        @Override protected int sizeOf(String key, Bitmap value) { return value.getByteCount(); }
    };
    private static final ExecutorService POOL = Executors.newFixedThreadPool(3);

    private OnlineArtwork() {}

    public static void load(String url, ImageView target) {
        if (target == null) return;
        if (url == null || url.isEmpty()) { target.setTag(null); return; }
        target.setTag(url);
        Bitmap cached = CACHE.get(url);
        if (cached != null) { target.setImageBitmap(cached); return; }
        final WeakReference<ImageView> ref = new WeakReference<>(target);
        POOL.execute(() -> {
            Bitmap bitmap = fetch(url);
            if (bitmap == null) return;
            CACHE.put(url, bitmap);
            ImageView view = ref.get();
            if (view != null && url.equals(view.getTag())) {
                view.post(() -> {
                    ImageView again = ref.get();
                    if (again != null && url.equals(again.getTag())) again.setImageBitmap(bitmap);
                });
            }
        });
    }

    /** Synchronous fetch — used from background threads (e.g. player artwork). */
    public static Bitmap fetch(String url) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(20000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            InputStream in = conn.getInputStream();
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 1;
            return BitmapFactory.decodeStream(in, null, options);
        } catch (Exception ignored) {
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }
}
