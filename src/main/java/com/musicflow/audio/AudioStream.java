package com.musicflow.audio;

import java.io.IOException;
import javax.sound.sampled.AudioFormat;

public interface AudioStream extends AutoCloseable {
    AudioFormat format();
    int read(short[] samples, int offset, int length) throws IOException;
    @Override
    void close() throws IOException;
}
