package com.musicflow.playlist;

import com.musicflow.model.Track;
import com.musicflow.visitor.LibraryVisitor;
import java.util.List;

public final class Playlist {
    private final String name;
    private final List<Track> tracks;

    Playlist(String name, List<Track> tracks) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Playlist name is required.");
        this.name = name.trim();
        this.tracks = List.copyOf(tracks);
    }

    public String getName() {
        return name;
    }

    public List<Track> getTracks() {
        return tracks;
    }

    public void accept(LibraryVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return name;
    }
}
