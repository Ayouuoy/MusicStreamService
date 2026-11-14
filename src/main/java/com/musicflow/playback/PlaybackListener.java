package com.musicflow.playback;

@FunctionalInterface
public interface PlaybackListener {
    void onPlaybackChanged(PlaybackEvent event);
}
