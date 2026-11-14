package com.musicflow.playback;

import com.musicflow.model.Track;
import java.util.List;

public interface PlaybackOrderStrategy {
    List<Track> order(List<Track> tracks);
}
