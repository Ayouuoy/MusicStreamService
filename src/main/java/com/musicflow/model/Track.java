package com.musicflow.model;

import com.musicflow.visitor.LibraryVisitor;
import java.net.URL;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public record Track(String title, String artist, URL source, long durationMillis,
                    List<LyricLine> lyrics) {
    public Track {
        if (title == null || title.isBlank()) throw new IllegalArgumentException("Track title is required.");
        Objects.requireNonNull(artist);
        Objects.requireNonNull(source);
        if (durationMillis < 0) throw new IllegalArgumentException("Duration cannot be negative.");
        lyrics = lyrics.stream().sorted(Comparator.comparingLong(LyricLine::timeMillis)).toList();
    }

    public void accept(LibraryVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return title + " — " + artist;
    }
}
