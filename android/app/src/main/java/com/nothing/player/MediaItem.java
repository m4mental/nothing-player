package com.nothing.player;

import java.io.Serializable;

public class MediaItem implements Serializable {
    public String id;
    public String title;
    public String path;
    public String contentUri;
    public long duration; // in ms
    public long size; // in bytes
    public String format;
    public String resolution;
    public String folder;
    public String artist;
    public String album;
    public String audioCodec;
    public String audioChannels;
    public boolean isFavorite;
    public long lastPosition;
    public long addedAt;
    public String type; // "video" or "audio"

    public MediaItem() {
        this.type = "video";
    }

    public MediaItem(String id, String title, String path, String contentUri, long duration, long size, String format, String folder, String type) {
        this.id = id;
        this.title = title;
        this.path = path;
        this.contentUri = contentUri;
        this.duration = duration;
        this.size = size;
        this.format = format;
        this.folder = folder;
        this.type = type;
        this.addedAt = System.currentTimeMillis();
    }
}
