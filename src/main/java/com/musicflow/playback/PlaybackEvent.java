package com.musicflow.playback;

import com.musicflow.model.Track;

public record PlaybackEvent(Track track, long positionMillis, State state, String message) {
    public enum State { STOPPED, PLAYING, PAUSED }
}
