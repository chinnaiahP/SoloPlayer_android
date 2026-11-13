package com.soloplayer.android.data.library;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import com.soloplayer.android.model.MediaItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Reads indexed audio and video records from Android MediaStore. */
public final class MediaLibraryRepository {
    private final Context context;

    public MediaLibraryRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    /** Performs a fresh index query. Call from a background executor. */
    public List<MediaItem> scan() {
        ArrayList<MediaItem> result = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= 29) {
            for (String volume : MediaStore.getExternalVolumeNames(context)) {
                scanCollection(MediaStore.Video.Media.getContentUri(volume), true, result);
                scanCollection(MediaStore.Audio.Media.getContentUri(volume), false, result);
            }
        } else {
            scanCollection(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, result);
            scanCollection(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, false, result);
        }
        Collections.sort(result, Comparator.comparingLong((MediaItem item) -> item.added).reversed());
        return result;
    }

    private void scanCollection(Uri base, boolean video, List<MediaItem> output) {
        ArrayList<String> columns = new ArrayList<>();
        Collections.addAll(columns, "_id", "_display_name", "date_added", "duration", "_size");
        columns.add(Build.VERSION.SDK_INT >= 29 ? "relative_path" : "_data");
        if (!video) Collections.addAll(columns, "artist", "album");

        try (Cursor cursor = context.getContentResolver().query(base,
                columns.toArray(new String[0]), null, null, "date_added DESC")) {
            if (cursor == null) return;
            int id = cursor.getColumnIndexOrThrow("_id");
            int name = cursor.getColumnIndexOrThrow("_display_name");
            int date = cursor.getColumnIndexOrThrow("date_added");
            int duration = cursor.getColumnIndex("duration");
            int size = cursor.getColumnIndex("_size");
            int path = cursor.getColumnIndex(Build.VERSION.SDK_INT >= 29 ? "relative_path" : "_data");
            int artist = video ? -1 : cursor.getColumnIndex("artist");
            int album = video ? -1 : cursor.getColumnIndex("album");
            while (cursor.moveToNext()) {
                String displayName = cursor.getString(name);
                Uri uri = ContentUris.withAppendedId(base, cursor.getLong(id));
                String folder = path >= 0 ? cursor.getString(path) : "";
                if (Build.VERSION.SDK_INT < 29 && folder != null) {
                    int slash = folder.lastIndexOf('/');
                    folder = slash > 0 ? folder.substring(0, slash) : "";
                }
                MediaItem item = new MediaItem(displayName == null ? "Media file" : displayName,
                        uri.toString(), video, folder, cursor.getLong(date),
                        duration >= 0 ? cursor.getLong(duration) : 0L,
                        artist >= 0 ? valueOrEmpty(cursor.getString(artist)) : "",
                        album >= 0 ? valueOrEmpty(cursor.getString(album)) : "");
                item.size = size >= 0 ? cursor.getLong(size) : 0L;
                output.add(item);
            }
        } catch (Exception ignored) {
            // A volume or collection can be unavailable; continue scanning the others.
        }
    }

    private static String valueOrEmpty(String value) {
        return value == null || "<unknown>".equals(value) ? "" : value;
    }
}
