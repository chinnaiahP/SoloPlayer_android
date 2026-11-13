package com.soloplayer.android.streaming;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses extended M3U IPTV lists without Android/UI dependencies. */
public final class M3uPlaylistParser {
    private static final List<String> SUPPORTED_SCHEMES = Arrays.asList(
            "http", "https", "rtsp", "rtsps", "rtmp", "rtp", "udp", "tcp", "srt",
            "ftp", "ftps", "sftp", "mms", "mmsh", "mmst");

    private M3uPlaylistParser() {}

    public static final class Channel {
        public final String name, url, userAgent, referer, logo, group, tvgId;
        Channel(String name, String url, String userAgent, String referer,
                String logo, String group, String tvgId) {
            this.name = name; this.url = url; this.userAgent = userAgent;
            this.referer = referer; this.logo = logo; this.group = group; this.tvgId = tvgId;
        }
    }

    public static final class Result {
        public final List<Channel> channels;
        public final int invalidEntries;
        Result(List<Channel> channels, int invalidEntries) {
            this.channels = channels; this.invalidEntries = invalidEntries;
        }
    }

    public static Result parse(BufferedReader reader, String playlistUrl, String fallbackName,
                               String fallbackAgent, String fallbackReferer) throws IOException {
        ArrayList<Channel> channels = new ArrayList<>();
        String title = null, logo = "", group = "", tvgId = "";
        String agent = fallbackAgent, referer = fallbackReferer;
        int invalidEntries = 0;
        boolean hls = false;
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.startsWith("\uFEFF")) line = line.substring(1).trim();
            if (line.isEmpty()) continue;
            String upper = line.toUpperCase(Locale.ROOT);
            if (upper.startsWith("#EXT-X-")) { hls = true; continue; }
            if (upper.startsWith("#EXTINF:")) {
                String metadata = line.substring(line.indexOf(':') + 1);
                int comma = unquotedComma(metadata);
                String attributes = comma >= 0 ? metadata.substring(0, comma) : metadata;
                String listedName = comma >= 0 ? metadata.substring(comma + 1).trim() : "";
                String tvgName = attribute(attributes, "tvg-name");
                title = !listedName.isEmpty() ? listedName : tvgName;
                logo = attribute(attributes, "tvg-logo");
                group = attribute(attributes, "group-title");
                tvgId = attribute(attributes, "tvg-id");
            } else if (upper.startsWith("#EXTGRP:")) {
                String listedGroup = line.substring(line.indexOf(':') + 1).trim();
                if (!listedGroup.isEmpty()) group = listedGroup;
            } else if (upper.startsWith("#EXTVLCOPT:")) {
                String option = line.substring(line.indexOf(':') + 1);
                int equals = option.indexOf('=');
                if (equals > 0) {
                    String key = option.substring(0, equals).trim().toLowerCase(Locale.ROOT);
                    String value = option.substring(equals + 1).trim();
                    if (key.endsWith("user-agent")) agent = value;
                    else if (key.endsWith("referrer") || key.endsWith("referer")) referer = value;
                }
            } else if (!line.startsWith("#") && !hls) {
                try {
                    String url = resolve(playlistUrl, line);
                    if (!isSupportedUrl(url)) throw new IOException("Unsupported stream URL");
                    String host = new URI(url).getHost();
                    String resolvedName = title == null || title.trim().isEmpty()
                            ? (host == null ? url : host) : title.trim();
                    channels.add(new Channel(resolvedName, url, agent, referer, logo, group, tvgId));
                } catch (Exception malformed) {
                    invalidEntries++;
                }
                title = null; logo = ""; group = ""; tvgId = "";
                agent = fallbackAgent; referer = fallbackReferer;
            }
        }
        if (hls) {
            String host;
            try { host = new URI(playlistUrl).getHost(); }
            catch (Exception ignored) { host = null; }
            String name = fallbackName == null || fallbackName.trim().isEmpty()
                    || fallbackName.equals(playlistUrl) ? (host == null ? playlistUrl : host) : fallbackName;
            channels.clear();
            channels.add(new Channel(name, playlistUrl, fallbackAgent, fallbackReferer, "", "", ""));
        }
        return new Result(channels, invalidEntries);
    }

    public static boolean isSupportedUrl(String value) {
        try {
            URI uri = new URI(value.trim());
            String scheme = uri.getScheme();
            if (scheme == null || !SUPPORTED_SCHEMES.contains(scheme.toLowerCase(Locale.ROOT))) return false;
            return scheme.equalsIgnoreCase("udp") || scheme.equalsIgnoreCase("rtp") || uri.getHost() != null;
        } catch (Exception ignored) { return false; }
    }

    private static String resolve(String playlistUrl, String entry) throws Exception {
        String value = entry.trim();
        if (value.isEmpty()) throw new IOException("Empty stream URL");
        if (value.matches("(?i)^[a-z][a-z0-9+.-]*:.*")) return value;
        return new URL(new URL(playlistUrl), value).toString();
    }

    private static int unquotedComma(String value) {
        char quote = 0;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (quote != 0) { if (c == quote) quote = 0; }
            else if (c == '"' || c == '\'') quote = c;
            else if (c == ',') return i;
        }
        return -1;
    }

    private static String attribute(String attributes, String name) {
        Matcher matcher = Pattern.compile("(?i)(?:^|\\s)" + Pattern.quote(name)
                + "\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\s,]*))").matcher(attributes);
        if (!matcher.find()) return "";
        for (int i = 1; i <= 3; i++) if (matcher.group(i) != null) return matcher.group(i).trim();
        return "";
    }
}
