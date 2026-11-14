package com.musicflow.visitor;

import com.musicflow.model.Track;
import com.musicflow.playlist.Playlist;

public final class TrackCountVisitor implements LibraryVisitor {
    private int count;

    @Override
    public void visit(Track track) {
        count++;
    }

    @Override
    public void visit(Playlist playlist) {
        playlist.getTracks().forEach(track -> track.accept(this));
    }

    public int getCount() {
        return count;
    }
}
