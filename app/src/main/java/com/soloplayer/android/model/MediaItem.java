package com.soloplayer.android.model;

/** Mutable media-library entry shared by browsing, queues, playback and streams. */
public final class MediaItem {
    public String name, uri, path, artist, album, agent, referer, logo, group, tvgId;
    public boolean video, network, folder;
    public boolean privateVault;
    public String privateVaultId;
    public long added, duration, size, positionMs;

    public MediaItem(String name, String uri, boolean video, String path, long added,
                     long duration, String artist, String album) {
        this.name = name;
        this.uri = uri;
        this.video = video;
        this.path = path;
        this.added = added;
        this.duration = duration;
        this.artist = artist;
        this.album = album;
        this.agent = "";
        this.referer = "";
        this.logo = "";
        this.group = "";
        this.tvgId = "";
    }

    public MediaItem(String name, String uri, boolean video) {
        this(name, uri, video, "", 0L, 0L, "", "");
    }
}
