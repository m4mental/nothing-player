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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MediaItem mediaItem = (MediaItem) o;
        if (id != null && mediaItem.id != null) return id.equals(mediaItem.id);
        if (path != null && mediaItem.path != null) return path.equals(mediaItem.path);
        return java.util.Objects.equals(contentUri, mediaItem.contentUri);
    }

    @Override
    public int hashCode() {
        if (id != null) return id.hashCode();
        if (path != null) return path.hashCode();
        return contentUri != null ? contentUri.hashCode() : 0;
    }
}
