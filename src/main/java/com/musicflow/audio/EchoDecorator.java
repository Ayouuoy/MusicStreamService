package com.musicflow.audio;

import java.io.IOException;
import java.util.Objects;
import java.util.function.BooleanSupplier;

public final class EchoDecorator extends AudioDecorator {
    private final BooleanSupplier enabled;
    private final short[] delay;
    private int position;

    public EchoDecorator(AudioStream wrapped) {
        this(wrapped, () -> true);
    }

    public EchoDecorator(AudioStream wrapped, BooleanSupplier enabled) {
        super(wrapped);
        this.enabled = Objects.requireNonNull(enabled);
        delay = new short[Math.max(1, Math.round(format().getSampleRate() * 0.18f))];
    }

    @Override
    public int read(short[] samples, int offset, int length) throws IOException {
        int count = wrapped.read(samples, offset, length);
        boolean active = enabled.getAsBoolean();
        for (int index = offset; index < offset + count; index++) {
            short dry = samples[index];
            if (active) samples[index] = clamp(dry + delay[position] * 0.4);
            delay[position] = active ? dry : 0;
            position = (position + 1) % delay.length;
        }
        return count;
    }
}
