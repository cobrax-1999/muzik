package com.example.musicplayer;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements SongAdapter.OnSongClickListener, MusicService.PlaybackListener {

    private static final int REQUEST_CODE_STORAGE_PERMISSION = 100;

    private SongAdapter adapter;
    private final List<Song> songs = new ArrayList<>();

    private TextView nowPlayingTitle;
    private ImageButton buttonPlayPause;

    private MusicService musicService;
    private boolean serviceBound = false;

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder musicBinder = (MusicService.MusicBinder) service;
            musicService = musicBinder.getService();
            musicService.setListener(MainActivity.this);
            musicService.setPlaylist(songs);
            serviceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RecyclerView recyclerView = findViewById(R.id.recycler_songs);
        nowPlayingTitle = findViewById(R.id.text_now_playing);
        buttonPlayPause = findViewById(R.id.button_play_pause);
        ImageButton buttonNext = findViewById(R.id.button_next);
        ImageButton buttonPrev = findViewById(R.id.button_prev);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SongAdapter(songs, this);
        recyclerView.setAdapter(adapter);

        buttonPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (serviceBound) {
                    musicService.togglePlayPause();
                }
            }
        });

        buttonNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (serviceBound) {
                    musicService.playNext();
                }
            }
        });

        buttonPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (serviceBound) {
                    musicService.playPrevious();
                }
            }
        });

        checkPermissionAndLoadSongs();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, MusicService.class);
        startService(intent);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (serviceBound) {
            musicService.setListener(null);
            unbindService(connection);
            serviceBound = false;
        }
    }

    private void checkPermissionAndLoadSongs() {
        String permission = Build.VERSION.SDK_INT >= 33
                ? Manifest.permission.READ_MEDIA_AUDIO
                : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            loadSongs();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{permission}, REQUEST_CODE_STORAGE_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadSongs();
            } else {
                Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void loadSongs() {
        songs.clear();
        songs.addAll(MusicLibraryLoader.loadDeviceSongs(this));
        adapter.notifyDataSetChanged();
        if (serviceBound) {
            musicService.setPlaylist(songs);
        }
    }

    @Override
    public void onSongClick(int position) {
        if (serviceBound) {
            musicService.playAt(position);
            adapter.setPlayingPosition(position);
        }
    }

    @Override
    public void onPlaybackStateChanged(boolean isPlaying, int songIndex) {
        buttonPlayPause.setImageResource(isPlaying
                ? android.R.drawable.ic_media_pause
                : android.R.drawable.ic_media_play);
        if (songIndex >= 0 && songIndex < songs.size()) {
            nowPlayingTitle.setText(songs.get(songIndex).getTitle());
            adapter.setPlayingPosition(songIndex);
        }
    }
}
