package com.example.musicplayer;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import java.util.ArrayList;
import java.util.List;

final class MusicLibraryLoader {

    private MusicLibraryLoader() {
    }

    static List<Song> loadDeviceSongs(Context context) {
        List<Song> songs = new ArrayList<>();
        ContentResolver resolver = context.getContentResolver();
        Uri collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DURATION
        };
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";

        Cursor cursor = resolver.query(collection, projection, selection, null, sortOrder);
        if (cursor == null) {
            return songs;
        }

        int idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
        int titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
        int artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
        int durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);

        while (cursor.moveToNext()) {
            long id = cursor.getLong(idCol);
            String title = cursor.getString(titleCol);
            String artist = cursor.getString(artistCol);
            long duration = cursor.getLong(durationCol);
            Uri songUri = Uri.withAppendedPath(collection, String.valueOf(id));
            songs.add(new Song(id, title, artist, duration, songUri));
        }
        cursor.close();

        return songs;
    }
}
