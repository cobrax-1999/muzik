package com.example.musicplayer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.media.app.NotificationCompat.MediaStyle;

import java.io.IOException;
import java.util.List;

public class MusicService extends Service
        implements MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener {

    static final String ACTION_PLAY_PAUSE = "com.example.musicplayer.action.PLAY_PAUSE";
    static final String ACTION_NEXT = "com.example.musicplayer.action.NEXT";
    static final String ACTION_PREV = "com.example.musicplayer.action.PREV";
    static final String ACTION_STOP = "com.example.musicplayer.action.STOP";

    private static final String CHANNEL_ID = "music_playback_channel";
    private static final int NOTIFICATION_ID = 1;

    interface PlaybackListener {
        void onPlaybackStateChanged(boolean isPlaying, int songIndex);
    }

    private final IBinder binder = new MusicBinder();
    private MediaPlayer mediaPlayer;
    private MediaSessionCompat mediaSession;
    private List<Song> playlist;
    private int currentIndex = -1;
    private PlaybackListener listener;
    private BroadcastReceiver controlsReceiver;

    class MusicBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        setupMediaSession();
        registerControlsReceiver();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            handleAction(intent.getAction());
        }
        return START_STICKY;
    }

    void setListener(PlaybackListener listener) {
        this.listener = listener;
    }

    void setPlaylist(List<Song> songs) {
        this.playlist = songs;
    }

    void playAt(int index) {
        if (playlist == null || index < 0 || index >= playlist.size()) {
            return;
        }
        currentIndex = index;
        startPlayback(playlist.get(index).getContentUri());
    }

    void togglePlayPause() {
        if (mediaPlayer == null) {
            return;
        }
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        } else {
            mediaPlayer.start();
        }
        updatePlaybackState();
        notifyListener();
        showNotification();
    }

    void playNext() {
        if (playlist == null || playlist.isEmpty()) {
            return;
        }
        playAt((currentIndex + 1) % playlist.size());
    }

    void playPrevious() {
        if (playlist == null || playlist.isEmpty()) {
            return;
        }
        playAt((currentIndex - 1 + playlist.size()) % playlist.size());
    }

    boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    private void startPlayback(Uri songUri) {
        releaseMediaPlayer();
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build());
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnPreparedListener(this);
        try {
            mediaPlayer.setDataSource(this, songUri);
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            stopSelf();
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.start();
        startForeground(NOTIFICATION_ID, buildNotification());
        updatePlaybackState();
        notifyListener();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        playNext();
    }

    private void handleAction(String action) {
        switch (action) {
            case ACTION_PLAY_PAUSE:
                togglePlayPause();
                break;
            case ACTION_NEXT:
                playNext();
                break;
            case ACTION_PREV:
                playPrevious();
                break;
            case ACTION_STOP:
                stopSelf();
                break;
            default:
                break;
        }
    }

    private void registerControlsReceiver() {
        controlsReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                handleAction(intent.getAction());
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_PLAY_PAUSE);
        filter.addAction(ACTION_NEXT);
        filter.addAction(ACTION_PREV);
        filter.addAction(ACTION_STOP);
        registerReceiver(controlsReceiver, filter);
    }

    private void setupMediaSession() {
        mediaSession = new MediaSessionCompat(this, "MusicService");
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
                | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mediaSession.setActive(true);
    }

    private void updatePlaybackState() {
        int state = isPlaying() ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED;
        long position = mediaPlayer != null ? mediaPlayer.getCurrentPosition() : 0;
        PlaybackStateCompat playbackState = new PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE
                        | PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                        | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
                .setState(state, position, 1.0f)
                .build();
        mediaSession.setPlaybackState(playbackState);
    }

    private void notifyListener() {
        if (listener != null) {
            listener.onPlaybackStateChanged(isPlaying(), currentIndex);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Music playback", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void showNotification() {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, buildNotification());
        }
    }

    private Notification buildNotification() {
        boolean hasSong = playlist != null && currentIndex >= 0 && currentIndex < playlist.size();
        String title = hasSong ? playlist.get(currentIndex).getTitle() : getString(R.string.app_name);
        String artist = hasSong ? playlist.get(currentIndex).getArtist() : "";
        int playPauseIcon = isPlaying() ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play;

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle(title)
                .setContentText(artist)
                .setOngoing(isPlaying())
                .addAction(android.R.drawable.ic_media_previous, "Prev", controlPendingIntent(ACTION_PREV))
                .addAction(playPauseIcon, "Play/Pause", controlPendingIntent(ACTION_PLAY_PAUSE))
                .addAction(android.R.drawable.ic_media_next, "Next", controlPendingIntent(ACTION_NEXT))
                .setStyle(new MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0, 1, 2))
                .build();
    }

    private PendingIntent controlPendingIntent(String action) {
        Intent intent = new Intent(action);
        intent.setPackage(getPackageName());
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getBroadcast(this, action.hashCode(), intent, flags);
    }

    private void releaseMediaPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    public void onDestroy() {
        releaseMediaPlayer();
        if (mediaSession != null) {
            mediaSession.release();
        }
        if (controlsReceiver != null) {
            unregisterReceiver(controlsReceiver);
        }
        super.onDestroy();
    }
}
