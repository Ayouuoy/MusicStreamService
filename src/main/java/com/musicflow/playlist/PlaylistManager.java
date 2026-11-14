package com.musicflow.playlist;

import com.musicflow.model.Track;
import java.util.ArrayList;
import java.util.List;

public final class PlaylistManager {
    private final List<Playlist> playlists = new ArrayList<>();

    public List<Playlist> getPlaylists() {
        return List.copyOf(playlists);
    }

    public Playlist create(String name) {
        Playlist playlist = new PlaylistBuilder().setName(name).build();
        if (playlists.stream().anyMatch(item -> item.getName().equalsIgnoreCase(playlist.getName()))) {
            throw new IllegalArgumentException("A playlist with this name already exists.");
        }
        playlists.add(playlist);
        return playlist;
    }

    public Playlist addTracks(Playlist playlist, List<Track> tracks) {
        int index = playlists.indexOf(playlist);
        if (index < 0) throw new IllegalArgumentException("Select an existing playlist.");
        PlaylistBuilder builder = new PlaylistBuilder().setName(playlist.getName());
        playlist.getTracks().forEach(builder::addTrack);
        tracks.forEach(builder::addTrack);
        Playlist updated = builder.build();
        playlists.set(index, updated);
        return updated;
    }
}
