package com.example.musicplayer;

import android.net.Uri;

class Song {

    private final long id;
    private final String title;
    private final String artist;
    private final long durationMs;
    private final Uri contentUri;

    Song(long id, String title, String artist, long durationMs, Uri contentUri) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.durationMs = durationMs;
        this.contentUri = contentUri;
    }

    long getId() {
        return id;
    }

    String getTitle() {
        return title;
    }

    String getArtist() {
        return artist;
    }

    long getDurationMs() {
        return durationMs;
    }

    Uri getContentUri() {
        return contentUri;
    }
}
