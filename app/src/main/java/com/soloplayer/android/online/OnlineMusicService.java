package com.soloplayer.android.online;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Anonymous YouTube Music catalog client. Uses the same public InnerTube
 * endpoints as the web client (search, browse, player) — no account required.
 * Ported from the desktop Solo Music Player's YouTubeMusicService.
 */
public final class OnlineMusicService {

    public static final class OnlineTrack {
        public final String videoId, title, artist, album, thumbnailUrl;
        public final long durationMs;
        OnlineTrack(String videoId, String title, String artist, String album, long durationMs, String thumbnailUrl) {
            this.videoId = videoId; this.title = title; this.artist = artist;
            this.album = album; this.durationMs = durationMs; this.thumbnailUrl = thumbnailUrl;
        }
        public String displayName() { return artist.isEmpty() || title.toLowerCase(Locale.ROOT).contains(artist.toLowerCase(Locale.ROOT)) ? title : title + " - " + artist; }
    }

    public static final class OnlinePlaylist {
        public final String browseId, title, subtitle, thumbnailUrl;
        OnlinePlaylist(String browseId, String title, String subtitle, String thumbnailUrl) {
            this.browseId = browseId; this.title = title; this.subtitle = subtitle; this.thumbnailUrl = thumbnailUrl;
        }
    }

    public static final class OnlineShelf {
        public final String title;
        public final List<OnlineTrack> tracks;
        public final List<OnlinePlaylist> playlists;
        OnlineShelf(String title, List<OnlineTrack> tracks, List<OnlinePlaylist> playlists) {
            this.title = title; this.tracks = tracks; this.playlists = playlists;
        }
    }

    public static final class OnlineMood {
        public final String title, browseId, params;
        OnlineMood(String title, String browseId, String params) {
            this.title = title; this.browseId = browseId; this.params = params;
        }
    }

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36";
    private static final String CLIENT_VERSION = "1.20260707.12.00";
    private static final Pattern VISITOR_PATTERN = Pattern.compile("^Cg[ts].*");
    private static final Pattern DURATION_PATTERN = Pattern.compile("\\d{1,2}:\\d{2}");
    private static final long RANGE_SIZE = 1024 * 1024L;
    private static final long CACHE_TTL_MS = 18L * 60 * 60 * 1000;

    private final java.io.File cacheDir;
    private volatile String guestVisitorData;

    /** @param cacheDir writable directory for downloaded audio (app cache dir recommended). */
    public OnlineMusicService(java.io.File cacheDir) {
        this.cacheDir = cacheDir;
    }

    // ------------------------------------------------------------------ API

    public List<OnlineTrack> search(String query, String countryCode) {
        if (query == null || query.trim().isEmpty()) return new ArrayList<>();
        JSONObject body = new JSONObject();
        try {
            body.put("context", context(countryCode));
            body.put("query", query.trim());
        } catch (JSONException e) { return new ArrayList<>(); }
        return distinctTracks(parseTracks(post("search", body)));
    }

    public List<OnlineShelf> home(String countryCode) {
        String country = normalizeCountry(countryCode);
        JSONObject body = new JSONObject();
        try {
            body.put("context", context(country));
            body.put("browseId", "FEmusic_charts");
        } catch (JSONException ignored) {}
        JSONObject root = post("browse", body);
        List<OnlineShelf> shelves = new ArrayList<>(parseShelves(root));
        for (String query : new String[]{"trending songs in " + country, "new music releases in " + country}) {
            try {
                List<OnlineTrack> results = search(query, country);
                if (!results.isEmpty()) shelves.add(new OnlineShelf(query, results, new ArrayList<OnlinePlaylist>()));
            } catch (Exception ignored) {}
        }
        if (!shelves.isEmpty()) return shelves.size() > 8 ? shelves.subList(0, 8) : shelves;
        try {
            body = new JSONObject();
            body.put("context", context(country));
            body.put("browseId", "FEmusic_home");
            shelves = parseShelves(post("browse", body));
            if (!shelves.isEmpty()) return shelves;
        } catch (Exception ignored) {}
        List<OnlineTrack> tracks = distinctTracks(parseTracks(root));
        if (!tracks.isEmpty()) {
            List<OnlineShelf> single = new ArrayList<>();
            single.add(new OnlineShelf("Featured in " + country, tracks, new ArrayList<OnlinePlaylist>()));
            return single;
        }
        throw new IllegalStateException("YouTube Music returned no playable songs. Try Search.");
    }

    public List<OnlineMood> moods(String countryCode) {
        JSONObject root = browse("FEmusic_moods_and_genres", null, countryCode);
        List<OnlineMood> chips = new ArrayList<>();
        for (JSONObject obj : collectObjects(root)) {
            JSONObject renderer = obj.optJSONObject("musicNavigationButtonRenderer");
            if (renderer == null) continue;
            OnlineMood mood = moodFrom(renderer);
            if (mood != null) chips.add(mood);
        }
        if (!chips.isEmpty()) return distinctMoods(chips);
        for (JSONObject obj : collectObjects(root)) {
            JSONObject renderer = obj.optJSONObject("musicTwoRowItemRenderer");
            if (renderer == null) continue;
            OnlineMood mood = moodFrom(renderer);
            if (mood != null) chips.add(mood);
        }
        return distinctMoods(chips);
    }

    public List<OnlineShelf> moodPlaylists(OnlineMood mood, String countryCode) {
        return parseShelves(browse(mood.browseId, mood.params, countryCode));
    }

    public List<OnlineTrack> playlistTracks(String browseId, String countryCode) {
        return distinctTracks(parseTracks(browse(browseId, null, countryCode)));
    }

    /** Resolve a direct AAC/MP4 stream URL. YouTube serves preview-only URLs
     * (~1 MiB prefix cap, verified byte-for-byte) for most anonymous clients,
     * so each candidate is probed at the file's last byte before use — the same
     * approach the Echo player uses. VISIONOS with full visionOS device context
     * is the only anonymous client measured to serve complete files reliably. */
    public String resolveAudio(String videoId) {
        IllegalStateException last = null;
        String[][] profiles = {
            {"VISIONOS", "0.1", "101", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Safari/605.1.15", "visionOS", "1.3.21O771", "Apple", "RealityDevice14,1", "0"},
            {"ANDROID_VR", "1.65.10", "28", "com.google.android.apps.youtube.vr.oculus/1.65.10 (Linux; U; Android 12L; eureka-user Build/SQ3A.220605.009.A1) gzip", "Android", "12L", "Oculus", "Quest 3", "32"},
            {"ANDROID", "20.10.38", "3", "com.google.android.youtube/20.10.38 (Linux; U; Android 11) gzip", "", "", "", "", "30"},
            {"IOS", "20.10.4", "5", "com.google.ios.youtube/20.10.4 (iPhone16,2; U; CPU iOS 18_3_2 like Mac OS X)", "iPhone", "18.3.2.22D82", "Apple", "iPhone16,2", "0"},
        };
        for (String[] p : profiles) {
            try {
                ResolvedStream stream = resolveAudio(videoId, p);
                if (streamReachable(stream.url, stream.contentLength)) return stream.url;
                last = new IllegalStateException("YouTube only provided a preview of this track (" + p[0] + ")");
            } catch (Exception e) {
                last = e instanceof IllegalStateException ? (IllegalStateException) e : new IllegalStateException(e.getMessage());
            }
        }
        throw last != null ? last : new IllegalStateException("YouTube did not provide a playable stream");
    }

    private static final class ResolvedStream {
        final String url;
        final long contentLength;
        ResolvedStream(String url, long contentLength) { this.url = url; this.contentLength = contentLength; }
    }

    /** googlevideo can mint URLs that only serve a fixed ~1 MiB prefix and 403
     * everything past it. Ask for the file's last byte: a URL that serves it is
     * not a truncated preview. When the length is unknown, probe the first chunk
     * instead. Network errors are treated as reachable — the real download has
     * its own retries. */
    private static boolean streamReachable(String url, long contentLength) {
        long start = contentLength > 0 ? contentLength - 1 : 0;
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(20000);
            conn.setRequestProperty("User-Agent", USER_AGENT);
            conn.setRequestProperty("Range", "bytes=" + start + "-" + start);
            int code = conn.getResponseCode();
            return code >= 200 && code < 300;
        } catch (Exception e) {
            return true;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private ResolvedStream resolveAudio(String videoId, String[] profile) {
        String visitor = ensureVisitorData();
        JSONObject body = new JSONObject();
        try {
            JSONObject client = new JSONObject();
            client.put("clientName", profile[0]); client.put("clientVersion", profile[1]);
            client.put("visitorData", visitor); client.put("gl", "US"); client.put("hl", "en");
            if (!profile[4].isEmpty()) client.put("osName", profile[4]);
            if (!profile[5].isEmpty()) client.put("osVersion", profile[5]);
            if (!profile[6].isEmpty()) client.put("deviceMake", profile[6]);
            if (!profile[7].isEmpty()) client.put("deviceModel", profile[7]);
            int sdk = Integer.parseInt(profile[8]);
            if (sdk > 0) client.put("androidSdkVersion", sdk);
            body.put("context", new JSONObject().put("client", client));
            body.put("videoId", videoId);
        } catch (JSONException e) { throw new IllegalStateException("Could not build player request"); }

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("User-Agent", profile[3]);
        headers.put("X-YouTube-Client-Name", profile[2]);
        headers.put("X-YouTube-Client-Version", profile[1]);
        JSONObject root = post("player", body, headers);

        JSONObject status = root.optJSONObject("playabilityStatus");
        if (status != null && !"OK".equals(status.optString("status", "OK"))) {
            String reason = status.optString("reason");
            throw new IllegalStateException(reason.isEmpty()
                ? "This track is unavailable for online playback (" + status.optString("status") + ")" : reason);
        }
        JSONArray formats = root.optJSONObject("streamingData") != null
            ? root.optJSONObject("streamingData").optJSONArray("adaptiveFormats") : null;
        if (formats == null) throw new IllegalStateException("YouTube did not provide a stream for this track");
        String best = null; long bestBitrate = -1, bestLength = 0;
        for (int i = 0; i < formats.length(); i++) {
            JSONObject f = formats.optJSONObject(i);
            if (f == null) continue;
            if (!f.optString("mimeType", "").startsWith("audio/mp4")) continue;
            String url = f.optString("url");
            if (!url.startsWith("https://")) continue;
            long bitrate = f.optLong("bitrate", 0);
            if (bitrate > bestBitrate) { bestBitrate = bitrate; best = url; bestLength = f.optLong("contentLength", 0); }
        }
        if (best == null) throw new IllegalStateException("YouTube did not provide a directly playable audio stream for this track");
        return new ResolvedStream(best, bestLength);
    }

    /** Resolve and download the track to a reusable local file. YouTube's CDN can reject
     * range-less media requests, so fetch in bounded 1 MB ranges (4 parallel workers),
     * like the desktop player and Echo's Android player do. */
    public java.io.File downloadAudio(String videoId) {
        String key = videoId.replaceAll("[^A-Za-z0-9_-]", "_");
        java.io.File target = new java.io.File(cacheDir, key + ".m4a");
        if (target.isFile() && target.length() > 0 && System.currentTimeMillis() - target.lastModified() < CACHE_TTL_MS) return target;
        String sourceUrl = resolveAudio(videoId);
        if (!cacheDir.exists() && !cacheDir.mkdirs()) throw new IllegalStateException("Could not create the online audio cache folder");
        IllegalStateException parallelError;
        java.io.File temporary;
        try {
            temporary = new java.io.File(cacheDir, key + "-" + java.util.UUID.randomUUID() + ".part");
            downloadRanges(sourceUrl, temporary, 4);
            return moveIntoPlace(temporary, target);
        } catch (IllegalStateException e) {
            parallelError = e;
        }
        // Some CDN nodes 403 concurrent connections — fall back to a single connection.
        try {
            temporary = new java.io.File(cacheDir, key + "-" + java.util.UUID.randomUUID() + ".part");
            downloadRanges(sourceUrl, temporary, 1);
            return moveIntoPlace(temporary, target);
        } catch (IllegalStateException e) {
            throw new IllegalStateException(e.getMessage() != null ? e.getMessage() : parallelError.getMessage(), e);
        }
    }

    private static java.io.File moveIntoPlace(java.io.File temporary, java.io.File target) {
        if (!temporary.isFile() || temporary.length() == 0L) { temporary.delete(); throw new IllegalStateException("YouTube returned an empty audio stream"); }
        if (!temporary.renameTo(target)) {
            if (target.exists() && !target.delete()) { temporary.delete(); throw new IllegalStateException("Could not store the downloaded track"); }
            if (!temporary.renameTo(target)) { temporary.delete(); throw new IllegalStateException("Could not store the downloaded track"); }
        }
        return target;
    }

    private static void downloadRanges(String sourceUrl, java.io.File temporary, int workers) {
        try {
            RangeChunk first = fetchRange(sourceUrl, 0, RANGE_SIZE - 1);
            if (first.bytes.length == 0) throw new IllegalStateException("YouTube returned an empty audio stream");
            final long total = first.total > 0 ? first.total : first.bytes.length;
            java.io.RandomAccessFile raf = new java.io.RandomAccessFile(temporary, "rw");
            try {
                raf.write(first.bytes);
                java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(workers);
                try {
                    List<java.util.concurrent.Future<?>> tasks = new ArrayList<>();
                    long next = first.bytes.length;
                    while (next < total) {
                        final long start = next;
                        final long end = Math.min(start + RANGE_SIZE, total) - 1;
                        tasks.add(pool.submit(() -> {
                            try {
                                byte[] buf = new byte[(int) (end - start + 1)];
                                int filled = 0;
                                while (filled < buf.length) {
                                    RangeChunk chunk = fetchRange(sourceUrl, start + filled, end);
                                    if (chunk.bytes.length == 0) throw new IllegalStateException("YouTube returned an empty audio range");
                                    System.arraycopy(chunk.bytes, 0, buf, filled, chunk.bytes.length);
                                    filled += chunk.bytes.length;
                                }
                                synchronized (raf) { raf.seek(start); raf.write(buf); }
                            } catch (RuntimeException e) {
                                throw e;
                            } catch (Exception e) {
                                throw new IllegalStateException("YouTube audio range " + start + "-" + end + " failed: " + e.getMessage(), e);
                            }
                        }));
                        next = end + 1;
                    }
                    for (java.util.concurrent.Future<?> task : tasks) task.get();
                } finally { pool.shutdownNow(); }
            } finally { raf.close(); }
        } catch (IllegalStateException e) {
            temporary.delete();
            throw e;
        } catch (Exception e) {
            temporary.delete();
            Throwable cause = e instanceof java.util.concurrent.ExecutionException && e.getCause() != null ? e.getCause() : e;
            if (cause instanceof IllegalStateException) throw (IllegalStateException) cause;
            throw new IllegalStateException("Could not download online audio: " + (cause.getMessage() != null ? cause.getMessage() : "network error"), e);
        }
    }

    private static final class RangeChunk {
        final byte[] bytes; final long total;
        RangeChunk(byte[] bytes, long total) { this.bytes = bytes; this.total = total; }
    }

    private static RangeChunk fetchRange(String sourceUrl, long start, long end) throws Exception {
        Exception last = null;
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                return fetchRangeOnce(sourceUrl, start, end);
            } catch (Exception e) {
                last = e;
                try { Thread.sleep(300L * (attempt + 1)); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
            }
        }
        throw last != null ? last : new IllegalStateException("YouTube audio range " + start + "-" + end + " failed");
    }

    private static RangeChunk fetchRangeOnce(String sourceUrl, long start, long end) throws Exception {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(sourceUrl).openConnection();
            conn.setConnectTimeout(20000);
            conn.setReadTimeout(120000);
            conn.setRequestProperty("User-Agent", USER_AGENT);
            conn.setRequestProperty("Accept", "audio/mp4,application/octet-stream;q=0.9,*/*;q=0.8");
            conn.setRequestProperty("Referer", "https://music.youtube.com/");
            conn.setRequestProperty("Range", "bytes=" + start + "-" + end);
            int code = conn.getResponseCode();
            byte[] bytes = readBytes(conn.getInputStream());
            if (code == 200) return new RangeChunk(bytes, bytes.length);
            if (code != 206) throw new IllegalStateException("YouTube audio CDN returned HTTP " + code + " while requesting bytes " + start + "-" + end);
            String contentRange = conn.getHeaderField("Content-Range");
            Matcher m = contentRange != null ? Pattern.compile("bytes \\d+-\\d+/(\\d+)").matcher(contentRange) : null;
            if (m == null || !m.matches()) throw new IllegalStateException("YouTube audio CDN returned an invalid range response");
            return new RangeChunk(bytes, Long.parseLong(m.group(1)));
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static byte[] readBytes(InputStream in) throws Exception {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream(1 << 16);
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
        in.close();
        return out.toByteArray();
    }

    // -------------------------------------------------------------- internals

    private JSONObject browse(String browseId, String params, String countryCode) {
        JSONObject body = new JSONObject();
        try {
            body.put("context", context(countryCode));
            body.put("browseId", browseId);
            if (params != null && !params.isEmpty()) body.put("params", params);
        } catch (JSONException ignored) {}
        return post("browse", body);
    }

    private JSONObject post(String path, JSONObject body) {
        return post(path, body, null);
    }

    private JSONObject post(String path, JSONObject body, Map<String, String> extraHeaders) {
        String visitor = ensureVisitorData();
        injectVisitor(body, visitor);
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("User-Agent", "player".equals(path)
            ? "com.google.android.apps.youtube.vr.oculus/1.65.10 (Linux; U; Android 12L; eureka-user Build/SQ3A.220605.009.A1) gzip" : USER_AGENT);
        headers.put("Accept", "application/json");
        headers.put("Content-Type", "application/json");
        headers.put("Origin", "https://music.youtube.com");
        headers.put("Referer", "https://music.youtube.com/");
        headers.put("X-YouTube-Client-Name", "player".equals(path) ? "28" : "67");
        headers.put("X-YouTube-Client-Version", "player".equals(path) ? "1.65.10" : CLIENT_VERSION);
        headers.put("X-Goog-Visitor-Id", visitor);
        if (extraHeaders != null) headers.putAll(extraHeaders);

        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL("https://music.youtube.com/youtubei/v1/" + path + "?prettyPrint=false").openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(20000);
            conn.setReadTimeout(30000);
            for (Map.Entry<String, String> h : headers.entrySet()) conn.setRequestProperty(h.getKey(), h.getValue());
            conn.setDoOutput(true);
            byte[] payload = body.toString().getBytes(StandardCharsets.UTF_8);
            OutputStream out = conn.getOutputStream();
            out.write(payload);
            out.close();
            int code = conn.getResponseCode();
            if (code < 200 || code > 299) throw new IllegalStateException("YouTube Music returned HTTP " + code);
            return new JSONObject(readFully(conn.getInputStream()));
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Could not reach YouTube Music: " + (e.getMessage() != null ? e.getMessage() : "network error"), e);
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private synchronized String ensureVisitorData() {
        if (guestVisitorData != null) return guestVisitorData;
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL("https://music.youtube.com/sw.js_data").openConnection();
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(20000);
            conn.setRequestProperty("User-Agent", USER_AGENT);
            conn.setRequestProperty("Accept", "*/*");
            int code = conn.getResponseCode();
            if (code < 200 || code > 299) throw new IllegalStateException("YouTube Music guest session returned HTTP " + code);
            String payload = readFully(conn.getInputStream());
            if (payload.startsWith(")]}'")) payload = payload.substring(4).trim();
            Object data = payload.startsWith("[") ? new JSONArray(payload) : new JSONObject(payload);
            List<String> primitives = new ArrayList<>();
            collectPrimitives(data, primitives);
            for (String s : primitives) {
                if (VISITOR_PATTERN.matcher(s).matches()) { guestVisitorData = s; return s; }
            }
            throw new IllegalStateException("YouTube Music did not issue a guest visitor token");
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Could not initialize a guest YouTube Music session: " + (e.getMessage() != null ? e.getMessage() : "network error"), e);
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static String readFully(InputStream in) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder(1 << 16);
        char[] buf = new char[8192];
        int n;
        while ((n = reader.read(buf)) != -1) sb.append(buf, 0, n);
        reader.close();
        return sb.toString();
    }

    private static JSONObject context(String countryCode) throws JSONException {
        return new JSONObject().put("client", new JSONObject()
            .put("clientName", "WEB_REMIX").put("clientVersion", CLIENT_VERSION)
            .put("hl", "en").put("gl", normalizeCountry(countryCode)));
    }

    private static String normalizeCountry(String c) {
        String up = c == null ? "US" : c.toUpperCase(Locale.ROOT);
        return up.matches("[A-Z]{2}") ? up : "US";
    }

    private static void injectVisitor(JSONObject body, String visitor) {
        try {
            JSONObject context = body.optJSONObject("context");
            if (context == null) return;
            JSONObject client = context.optJSONObject("client");
            if (client == null) return;
            client.put("visitorData", visitor);
        } catch (JSONException ignored) {}
    }

    // ------------------------------------------------------------- parsing

    private List<OnlineShelf> parseShelves(JSONObject root) {
        List<OnlineShelf> shelves = new ArrayList<>();
        for (JSONObject obj : collectObjects(root)) {
            JSONObject renderer = obj.optJSONObject("musicCarouselShelfRenderer");
            if (renderer == null) continue;
            JSONObject header = renderer.optJSONObject("header");
            Object titleEl = header != null ? findValue(header, "title") : null;
            String title = titleEl != null ? renderedText(titleEl) : "";
            if (title.isEmpty()) continue;
            Object contents = renderer.opt("contents");
            List<OnlineTrack> tracks = contents != null ? distinctTracks(parseTracks(contents)) : new ArrayList<OnlineTrack>();
            List<OnlinePlaylist> playlists = contents != null ? parsePlaylists(contents) : new ArrayList<OnlinePlaylist>();
            if (!tracks.isEmpty() || !playlists.isEmpty()) shelves.add(new OnlineShelf(title, tracks, playlists));
        }
        return shelves;
    }

    private OnlineMood moodFrom(JSONObject renderer) {
        Object endpointEl = findValue(renderer, "browseEndpoint");
        if (!(endpointEl instanceof JSONObject)) return null;
        JSONObject endpoint = (JSONObject) endpointEl;
        String browseId = endpoint.optString("browseId");
        if (!browseId.startsWith("FEmusic_moods")) return null;
        String params = endpoint.optString("params", null);
        Object btnText = renderer.opt("buttonText");
        String title = btnText != null ? renderedText(btnText) : "";
        if (title.isEmpty()) {
            Object titleEl = renderer.opt("title");
            if (titleEl != null) title = renderedText(titleEl);
        }
        return title.isEmpty() ? null : new OnlineMood(title, browseId, params);
    }

    private List<OnlinePlaylist> parsePlaylists(Object element) {
        List<OnlinePlaylist> playlists = new ArrayList<>();
        for (JSONObject item : collectObjects(element)) {
            JSONObject renderer = item.optJSONObject("musicTwoRowItemRenderer");
            if (renderer == null) continue;
            Object endpointEl = renderer.opt("navigationEndpoint");
            Object browseEp = endpointEl != null ? findValue(endpointEl, "browseEndpoint") : null;
            if (!(browseEp instanceof JSONObject)) continue;
            String browseId = ((JSONObject) browseEp).optString("browseId");
            if (!(browseId.startsWith("VL") || browseId.startsWith("OLAK5uy"))) continue;
            Object titleEl = renderer.opt("title");
            String title = titleEl != null ? renderedText(titleEl) : "";
            if (title.isEmpty()) continue;
            Object subEl = renderer.opt("subtitle");
            String subtitle = subEl != null ? renderedText(subEl) : "";
            Object thumbHolder = renderer.opt("thumbnail");
            if (thumbHolder == null) thumbHolder = renderer.opt("thumbnailRenderer");
            String thumb = lastThumbnail(thumbHolder != null ? findValue(thumbHolder, "thumbnails") : null);
            playlists.add(new OnlinePlaylist(browseId, title, subtitle, thumb));
        }
        Map<String, OnlinePlaylist> unique = new LinkedHashMap<>();
        for (OnlinePlaylist p : playlists) unique.put(p.browseId, p);
        return new ArrayList<>(unique.values());
    }

    private List<OnlineTrack> parseTracks(Object element) {
        List<OnlineTrack> tracks = new ArrayList<>();
        for (JSONObject item : collectObjects(element)) {
            JSONObject renderer = item.optJSONObject("musicResponsiveListItemRenderer");
            if (renderer == null) renderer = item.optJSONObject("musicTwoRowItemRenderer");
            if (renderer == null) renderer = item.optJSONObject("musicMultiRowListItemRenderer");
            if (renderer == null) continue;
            Object videoIdEl = findValue(renderer, "videoId");
            String videoId = primitiveText(videoIdEl);
            if (videoId == null || videoId.isEmpty()) continue;
            JSONArray columns = renderer.optJSONArray("flexColumns");
            String title = "";
            if (columns != null && columns.length() > 0) title = renderedText(columns.opt(0));
            if (title.isEmpty()) {
                Object t = renderer.opt("title");
                if (t != null) title = renderedText(t);
            }
            if (title.isEmpty()) continue;
            String subtitle = "";
            if (columns != null && columns.length() > 1) subtitle = renderedText(columns.opt(1));
            String[] segments = subtitle.split("\\s*[\\u2022\\u00b7]\\s*");
            String artist = "YouTube Music", album = "";
            List<String> parts = new ArrayList<>();
            for (String s : segments) { String t = s.trim(); if (!t.isEmpty()) parts.add(t); }
            if (!parts.isEmpty()) artist = parts.get(0);
            if (parts.size() > 1) album = parts.get(1);
            String thumb = lastThumbnail(findValue(renderer, "thumbnails"));
            long duration = 0;
            for (JSONObject inner : collectObjects(renderer)) {
                for (String key : keys(inner)) {
                    Object v = inner.opt(key);
                    String text = primitiveText(v);
                    if (text != null && DURATION_PATTERN.matcher(text).matches()) { duration = parseDuration(text); break; }
                }
                if (duration > 0) break;
            }
            tracks.add(new OnlineTrack(videoId, title, artist, album, duration, thumb));
        }
        return tracks;
    }

    private static String lastThumbnail(Object thumbnailsEl) {
        if (!(thumbnailsEl instanceof JSONArray)) return null;
        JSONArray arr = (JSONArray) thumbnailsEl;
        String url = null;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject t = arr.optJSONObject(i);
            if (t != null) url = t.optString("url", url);
        }
        return url;
    }

    private static String primitiveText(Object el) {
        if (el instanceof String) return (String) el;
        if (el instanceof Number || el instanceof Boolean) return String.valueOf(el);
        return null;
    }

    private static String renderedText(Object element) {
        if (element == null || element == JSONObject.NULL) return "";
        if (element instanceof String || element instanceof Number || element instanceof Boolean) return String.valueOf(element);
        if (element instanceof JSONArray) {
            JSONArray arr = (JSONArray) element;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < arr.length(); i++) sb.append(renderedText(arr.opt(i)));
            return sb.toString();
        }
        if (element instanceof JSONObject) {
            JSONObject obj = (JSONObject) element;
            JSONArray runs = obj.optJSONArray("runs");
            if (runs != null) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < runs.length(); i++) {
                    JSONObject run = runs.optJSONObject(i);
                    if (run != null) sb.append(renderedText(run.opt("text")));
                }
                return sb.toString();
            }
            if (obj.has("text")) return renderedText(obj.opt("text"));
            if (obj.has("simpleText")) return obj.optString("simpleText");
            for (String key : keys(obj)) {
                String s = renderedText(obj.opt(key));
                if (!s.isEmpty()) return s;
            }
        }
        return "";
    }

    /** Depth-first list of every JSONObject in the tree (root included). */
    private static List<JSONObject> collectObjects(Object node) {
        List<JSONObject> out = new ArrayList<>();
        collectObjects(node, out);
        return out;
    }

    private static void collectObjects(Object node, List<JSONObject> out) {
        if (node instanceof JSONObject) {
            JSONObject obj = (JSONObject) node;
            out.add(obj);
            for (String key : keys(obj)) collectObjects(obj.opt(key), out);
        } else if (node instanceof JSONArray) {
            JSONArray arr = (JSONArray) node;
            for (int i = 0; i < arr.length(); i++) collectObjects(arr.opt(i), out);
        }
    }

    private static void collectPrimitives(Object node, List<String> out) {
        if (node instanceof JSONObject) {
            JSONObject obj = (JSONObject) node;
            for (String key : keys(obj)) collectPrimitives(obj.opt(key), out);
        } else if (node instanceof JSONArray) {
            JSONArray arr = (JSONArray) node;
            for (int i = 0; i < arr.length(); i++) collectPrimitives(arr.opt(i), out);
        } else if (node instanceof String) {
            out.add((String) node);
        }
    }

    /** First non-null value of {@code key} anywhere in the subtree. */
    private static Object findValue(Object node, String key) {
        if (node instanceof JSONObject) {
            JSONObject obj = (JSONObject) node;
            if (obj.has(key)) return obj.opt(key);
            for (String k : keys(obj)) {
                Object found = findValue(obj.opt(k), key);
                if (found != null) return found;
            }
        } else if (node instanceof JSONArray) {
            JSONArray arr = (JSONArray) node;
            for (int i = 0; i < arr.length(); i++) {
                Object found = findValue(arr.opt(i), key);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static List<String> keys(JSONObject obj) {
        List<String> out = new ArrayList<>();
        java.util.Iterator<String> it = obj.keys();
        while (it.hasNext()) out.add(it.next());
        return out;
    }

    private static long parseDuration(String text) {
        long sum = 0;
        for (String part : text.split(":")) {
            try { sum = sum * 60 + Long.parseLong(part); } catch (NumberFormatException ignored) {}
        }
        return sum * 1000L;
    }

    private static List<OnlineTrack> distinctTracks(List<OnlineTrack> tracks) {
        Map<String, OnlineTrack> unique = new LinkedHashMap<>();
        for (OnlineTrack t : tracks) unique.put(t.videoId, t);
        return new ArrayList<>(unique.values());
    }

    private static List<OnlineMood> distinctMoods(List<OnlineMood> moods) {
        Map<String, OnlineMood> unique = new LinkedHashMap<>();
        for (OnlineMood m : moods) unique.put(m.title, m);
        return new ArrayList<>(unique.values());
    }
}
