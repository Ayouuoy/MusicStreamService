package com.musicflow.model;

import java.util.Objects;

public record LyricLine(long timeMillis, String text) {
    public LyricLine {
        if (timeMillis < 0) throw new IllegalArgumentException("Lyric time cannot be negative.");
        Objects.requireNonNull(text);
    }
}
