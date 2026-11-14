package com.musicflow.playlist;

import com.musicflow.model.Track;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class PlaylistBuilder {
    private String name;
    private final List<Track> tracks = new ArrayList<>();

    public PlaylistBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public PlaylistBuilder addTrack(Track track) {
        Objects.requireNonNull(track);
        if (!tracks.contains(track)) tracks.add(track);
        return this;
    }

    public Playlist build() {
        return new Playlist(name, tracks);
    }
}
