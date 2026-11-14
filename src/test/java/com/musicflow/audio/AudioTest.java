package com.musicflow.audio;

import java.io.IOException;
import java.util.Arrays;
import javax.sound.sampled.AudioFormat;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AudioTest {
    private static final class Samples implements AudioStream {
        private final short[] data;
        private int position;
        private boolean closed;

        Samples(short... data) {
            this.data = data;
        }

        @Override
        public AudioFormat format() {
            return new AudioFormat(1000, 16, 1, true, false);
        }

        @Override
        public int read(short[] samples, int offset, int length) {
            if (position == data.length) return -1;
            int count = Math.min(length, data.length - position);
            System.arraycopy(data, position, samples, offset, count);
            position += count;
            return count;
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    @Test
    void volumeChangesSamplesAtRequestedOffsetAndPropagatesClose() throws IOException {
        Samples original = new Samples((short) 1000, (short) -1000, (short) 32000);
        AudioStream stream = new VolumeDecorator(original, 0.5);
        short[] result = new short[5];
        assertEquals(3, stream.read(result, 1, 3));
        assertArrayEquals(new short[]{0, 500, -500, 16000, 0}, result);
        assertEquals(-1, stream.read(result, 0, 1));
        stream.close();
        assertTrue(original.closed);
        assertThrows(IllegalArgumentException.class, () -> new VolumeDecorator(new Samples(), -1));
    }

    @Test
    void echoKeepsDelayAcrossReadsAndComposesWithVolume() throws IOException {
        short[] data = new short[200];
        data[0] = 10000;
        AudioStream stream = new VolumeDecorator(new EchoDecorator(new Samples(data)), 0.5);
        short[] first = new short[100];
        short[] second = new short[100];
        assertEquals(100, stream.read(first, 0, 100));
        assertEquals(100, stream.read(second, 0, 100));
        assertEquals(5000, first[0]);
        assertEquals(2000, second[80]);
        assertEquals(0, second[79]);
        assertEquals(-1, stream.read(second, 0, 100));
    }

    @Test
    void echoClipsSafelyAndCanBeDisabled() throws IOException {
        short[] data = new short[200];
        Arrays.fill(data, Short.MAX_VALUE);
        short[] output = new short[200];
        new EchoDecorator(new Samples(data)).read(output, 0, 200);
        assertEquals(Short.MAX_VALUE, output[180]);
        new EchoDecorator(new Samples(data), () -> false).read(output, 0, 200);
        assertArrayEquals(data, output);
    }

    @Test
    void volumeAndEchoChangeDynamicallyWithoutReopeningTheStream() throws IOException {
        double[] gain = {1};
        boolean[] echo = {false};
        short[] data = new short[540];
        data[0] = 10000;
        data[180] = 10000;
        AudioStream stream = new VolumeDecorator(new EchoDecorator(new Samples(data), () -> echo[0]), () -> gain[0]);
        short[] output = new short[180];
        stream.read(output, 0, 180);
        assertEquals(10000, output[0]);
        gain[0] = 0.5;
        echo[0] = true;
        stream.read(output, 0, 180);
        assertEquals(5000, output[0]);
        stream.read(output, 0, 180);
        assertEquals(2000, output[0]);
    }

    @Test
    void realWavDecodesToDefinedPcmAndMuteProducesSilence() throws Exception {
        try (AudioStream raw = new WavAudioStream(getClass().getResource("/audio/morning.wav"))) {
            assertEquals(44100, raw.format().getSampleRate());
            assertEquals(16, raw.format().getSampleSizeInBits());
            assertEquals(1, raw.format().getChannels());
            assertFalse(raw.format().isBigEndian());
            short[] samples = new short[44100];
            int count = raw.read(samples, 0, samples.length);
            assertTrue(count > 0);
            boolean nonzero = false;
            for (short sample : samples) nonzero |= sample != 0;
            assertTrue(nonzero);
        }
        try (AudioStream mute = new VolumeDecorator(new WavAudioStream(getClass().getResource("/audio/morning.wav")), 0)) {
            short[] samples = new short[1024];
            assertEquals(1024, mute.read(samples, 0, samples.length));
            assertArrayEquals(new short[1024], samples);
        }
    }
}
