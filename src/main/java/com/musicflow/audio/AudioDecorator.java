package com.musicflow.audio;

import java.io.IOException;
import java.util.Objects;
import javax.sound.sampled.AudioFormat;

public abstract class AudioDecorator implements AudioStream {
    protected final AudioStream wrapped;

    protected AudioDecorator(AudioStream wrapped) {
        this.wrapped = Objects.requireNonNull(wrapped);
    }

    @Override
    public AudioFormat format() {
        return wrapped.format();
    }

    protected short clamp(double sample) {
        return (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, Math.round(sample)));
    }

    @Override
    public void close() throws IOException {
        wrapped.close();
    }
}
