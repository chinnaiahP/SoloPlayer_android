package com.soloplayer.android.data.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import com.soloplayer.android.model.MediaItem;
import com.soloplayer.android.streaming.M3uPlaylistParser;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/** Persistence boundary for saved network channels, including legacy-data migration. */
public final class StreamBookmarkRepository {
    private static final String PREFS = "solo_player";
    private static final String KEY = "stream_bookmarks_v1";
    private final SharedPreferences preferences;

    public StreamBookmarkRepository(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Loads the dedicated key, or migrates records from the old shared JSON document once. */
    public List<MediaItem> load(JSONArray legacyRecords) {
        ArrayList<MediaItem> result = new ArrayList<>();
        String saved = preferences.getString(KEY, null);
        JSONArray records;
        if (saved != null) {
            try { records = new JSONArray(saved); }
            catch (Exception invalid) { records = new JSONArray(); }
        } else {
            records = legacyRecords == null ? new JSONArray() : legacyRecords;
        }
        for (int i = 0; i < records.length(); i++) {
            try {
                JSONObject item = records.getJSONObject(i);
                String url = item.optString("uri", "");
                if (!M3uPlaylistParser.isSupportedUrl(url)) continue;
                boolean duplicate = false;
                for (MediaItem present : result) if (present.uri.equalsIgnoreCase(url)) { duplicate = true; break; }
                if (duplicate) continue;
                String name = item.optString("name", "").trim();
                if (name.isEmpty()) name = defaultName(url);
                MediaItem media = new MediaItem(name, url, true, "Network", 0L, 0L, "", "");
                media.network = true;
                media.agent = item.optString("agent", "");
                media.referer = item.optString("referer", "");
                media.logo = item.optString("logo", "");
                media.group = item.optString("group", "");
                media.tvgId = item.optString("tvgId", "");
                result.add(media);
            } catch (Exception malformed) {
                // Ignore a corrupt record while preserving the remaining bookmarks.
            }
        }
        if (saved == null && !result.isEmpty()) save(result);
        return result;
    }

    public void save(List<MediaItem> bookmarks) {
        JSONArray records = new JSONArray();
        for (MediaItem item : bookmarks) {
            try {
                JSONObject record = new JSONObject();
                record.put("name", item.name);
                record.put("uri", item.uri);
                record.put("agent", item.agent);
                record.put("referer", item.referer);
                record.put("logo", item.logo);
                record.put("group", item.group);
                record.put("tvgId", item.tvgId);
                records.put(record);
            } catch (Exception ignored) {}
        }
        preferences.edit().putString(KEY, records.toString()).apply();
    }

    private static String defaultName(String url) {
        try {
            String host = new URI(url).getHost();
            if (host != null && !host.isEmpty()) return host;
        } catch (Exception ignored) {}
        String segment = Uri.parse(url).getLastPathSegment();
        return segment == null || segment.isEmpty() ? url : segment;
    }
}
