package com.musicflow.visitor;

import com.musicflow.model.Track;
import com.musicflow.playlist.Playlist;

public final class DurationVisitor implements LibraryVisitor {
    private long totalMillis;

    @Override
    public void visit(Track track) {
        totalMillis += track.durationMillis();
    }

    @Override
    public void visit(Playlist playlist) {
        playlist.getTracks().forEach(track -> track.accept(this));
    }

    public long getTotalMillis() {
        return totalMillis;
    }
}
