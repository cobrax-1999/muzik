package com.example.musicplayer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {

    interface OnSongClickListener {
        void onSongClick(int position);
    }

    private final List<Song> songs;
    private final OnSongClickListener listener;
    private int playingPosition = RecyclerView.NO_POSITION;

    SongAdapter(List<Song> songs, OnSongClickListener listener) {
        this.songs = songs;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_song, parent, false);
        return new SongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        Song song = songs.get(position);
        holder.title.setText(song.getTitle());
        holder.artist.setText(song.getArtist());
        holder.duration.setText(formatDuration(song.getDurationMs()));
        holder.itemView.setSelected(position == playingPosition);
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    void setPlayingPosition(int position) {
        int previous = playingPosition;
        playingPosition = position;
        if (previous != RecyclerView.NO_POSITION) {
            notifyItemChanged(previous);
        }
        notifyItemChanged(playingPosition);
    }

    private static String formatDuration(long durationMs) {
        long totalSeconds = durationMs / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
    }

    class SongViewHolder extends RecyclerView.ViewHolder {

        final TextView title;
        final TextView artist;
        final TextView duration;

        SongViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.text_song_title);
            artist = itemView.findViewById(R.id.text_song_artist);
            duration = itemView.findViewById(R.id.text_song_duration);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION && listener != null) {
                        listener.onSongClick(position);
                    }
                }
            });
        }
    }
}
