package com.musicflow.audio;

import java.io.IOException;
import java.util.Objects;
import java.util.function.DoubleSupplier;

public final class VolumeDecorator extends AudioDecorator {
    private final DoubleSupplier gain;

    public VolumeDecorator(AudioStream wrapped, double gain) {
        this(wrapped, () -> gain);
        if (!Double.isFinite(gain) || gain < 0 || gain > 1) {
            throw new IllegalArgumentException("Volume must be between 0 and 1.");
        }
    }

    public VolumeDecorator(AudioStream wrapped, DoubleSupplier gain) {
        super(wrapped);
        this.gain = Objects.requireNonNull(gain);
    }

    @Override
    public int read(short[] samples, int offset, int length) throws IOException {
        int count = wrapped.read(samples, offset, length);
        double level = Math.max(0, Math.min(1, gain.getAsDouble()));
        for (int index = offset; index < offset + count; index++) {
            samples[index] = clamp(samples[index] * level);
        }
        return count;
    }
}
