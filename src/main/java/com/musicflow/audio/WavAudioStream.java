package com.musicflow.audio;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

public final class WavAudioStream implements AudioStream {
    public static final AudioFormat PCM = new AudioFormat(44100, 16, 1, true, false);
    private final AudioInputStream input;

    public WavAudioStream(URL source) throws IOException, UnsupportedAudioFileException {
        AudioInputStream original = AudioSystem.getAudioInputStream(Objects.requireNonNull(source));
        try {
            input = AudioSystem.getAudioInputStream(PCM, original);
        } catch (IllegalArgumentException exception) {
            original.close();
            throw new IOException("WAV cannot be converted to 44.1 kHz, 16-bit mono PCM.", exception);
        }
    }

    @Override
    public AudioFormat format() {
        return PCM;
    }

    @Override
    public int read(short[] samples, int offset, int length) throws IOException {
        Objects.checkFromIndexSize(offset, length, samples.length);
        if (length == 0) return 0;
        byte[] bytes = new byte[length * 2];
        int count = input.read(bytes);
        if (count < 0) return -1;
        for (int index = 0; index < count / 2; index++) {
            samples[offset + index] = (short) ((bytes[index * 2] & 255) | (bytes[index * 2 + 1] << 8));
        }
        return count / 2;
    }

    @Override
    public void close() throws IOException {
        input.close();
    }
}
