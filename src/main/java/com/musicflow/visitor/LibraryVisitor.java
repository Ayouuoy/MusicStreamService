package com.musicflow.visitor;

import com.musicflow.model.Track;
import com.musicflow.playlist.Playlist;

public interface LibraryVisitor {
    void visit(Track track);
    void visit(Playlist playlist);
}
