package com.musicflow.playback;

import com.musicflow.model.Track;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public final class ShuffleStrategy implements PlaybackOrderStrategy {
    private final Random random;

    public ShuffleStrategy() {
        this(new Random());
    }

    public ShuffleStrategy(Random random) {
        this.random = Objects.requireNonNull(random);
    }

    @Override
    public List<Track> order(List<Track> tracks) {
        List<Track> result = new ArrayList<>(tracks);
        Collections.shuffle(result, random);
        return List.copyOf(result);
    }
}
