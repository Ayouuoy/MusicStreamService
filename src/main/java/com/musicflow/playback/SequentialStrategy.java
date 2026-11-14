package com.musicflow.playback;

import com.musicflow.model.Track;
import java.util.List;

public final class SequentialStrategy implements PlaybackOrderStrategy {
    @Override
    public List<Track> order(List<Track> tracks) {
        return List.copyOf(tracks);
    }
}
