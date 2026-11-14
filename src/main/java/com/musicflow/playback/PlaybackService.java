package com.musicflow.playback;

import com.musicflow.audio.AudioStream;
import com.musicflow.audio.EchoDecorator;
import com.musicflow.audio.VolumeDecorator;
import com.musicflow.audio.WavAudioStream;
import com.musicflow.model.Track;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import javax.swing.SwingUtilities;

public final class PlaybackService implements AutoCloseable {
    private final List<PlaybackListener> listeners = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "musicflow-audio");
        thread.setDaemon(true);
        return thread;
    });
    private final Function<AudioFormat, SourceDataLine> lineProvider;
    private final short[] samples = new short[882];
    private final byte[] bytes = new byte[1764];
    private volatile PlaybackEvent event = new PlaybackEvent(null, 0, PlaybackEvent.State.STOPPED, "Ready");
    private volatile double volume = 0.7;
    private volatile boolean echo;
    private volatile boolean closed;
    private PlaybackOrderStrategy strategy = new SequentialStrategy();
    private List<Track> originalQueue = List.of();
    private List<Track> queue = List.of();
    private int index = -1;
    private AudioStream stream;
    private SourceDataLine line;
    private long submittedFrames;
    private boolean endOfStream;

    public PlaybackService() {
        this(format -> {
            try {
                return AudioSystem.getSourceDataLine(format);
            } catch (Exception exception) {
                throw new IllegalStateException("No audio output is available.", exception);
            }
        });
    }

    PlaybackService(Function<AudioFormat, SourceDataLine> lineProvider) {
        this.lineProvider = Objects.requireNonNull(lineProvider);
        worker.scheduleWithFixedDelay(this::tick, 0, 20, TimeUnit.MILLISECONDS);
    }

    public void subscribe(PlaybackListener listener) {
        Objects.requireNonNull(listener);
        if (!listeners.contains(listener)) listeners.add(listener);
        SwingUtilities.invokeLater(() -> {
            if (listeners.contains(listener)) listener.onPlaybackChanged(event);
        });
    }

    public void unsubscribe(PlaybackListener listener) {
        listeners.remove(listener);
    }

    public PlaybackEvent getEvent() {
        return event;
    }

    public void play(List<Track> tracks, Track selected) {
        List<Track> requested = List.copyOf(tracks);
        if (requested.isEmpty()) throw new IllegalArgumentException("Select at least one track.");
        if (!requested.contains(selected)) throw new IllegalArgumentException("Select a track from this collection.");
        submit(() -> {
            if (requested.equals(originalQueue) && selected.equals(event.track()) && line != null) {
                resumePlayback();
                return;
            }
            originalQueue = requested;
            queue = strategy.order(originalQueue);
            index = queue.indexOf(selected);
            startTrack();
        });
    }

    public void resume() {
        submit(() -> {
            if (line != null) resumePlayback();
            else if (index >= 0) startTrack();
        });
    }

    public void pause() {
        submit(() -> {
            if (line == null || event.state() != PlaybackEvent.State.PLAYING) return;
            line.stop();
            publish(PlaybackEvent.State.PAUSED, positionMillis(), "Paused");
        });
    }

    public void stop() {
        submit(() -> {
            releaseAudio();
            publish(PlaybackEvent.State.STOPPED, 0, "Stopped");
        });
    }

    public void next() {
        submit(this::advance);
    }

    public void previous() {
        submit(() -> {
            if (queue.isEmpty()) return;
            index = Math.max(0, index - 1);
            startTrack();
        });
    }

    public void setStrategy(PlaybackOrderStrategy strategy) {
        Objects.requireNonNull(strategy);
        submit(() -> {
            this.strategy = strategy;
            queue = strategy.order(originalQueue);
            index = event.track() == null ? -1 : queue.indexOf(event.track());
        });
    }

    public void setVolume(double volume) {
        if (!Double.isFinite(volume) || volume < 0 || volume > 1) {
            throw new IllegalArgumentException("Volume must be between 0 and 1.");
        }
        this.volume = volume;
    }

    public void setEcho(boolean echo) {
        this.echo = echo;
    }

    private synchronized void submit(Runnable action) {
        if (closed) return;
        worker.execute(() -> {
            try {
                action.run();
            } catch (Exception exception) {
                fail(exception);
            }
        });
    }

    private void startTrack() {
        releaseAudio();
        Track track = queue.get(index);
        event = new PlaybackEvent(track, 0, PlaybackEvent.State.STOPPED, "Loading");
        try {
            stream = new VolumeDecorator(new EchoDecorator(new WavAudioStream(track.source()), () -> echo), () -> volume);
            line = lineProvider.apply(stream.format());
            line.open(stream.format(), 8820);
            submittedFrames = 0;
            endOfStream = false;
            line.start();
            publish(PlaybackEvent.State.PLAYING, 0, "Playing");
        } catch (Exception exception) {
            fail(exception);
        }
    }

    private void resumePlayback() {
        line.start();
        publish(PlaybackEvent.State.PLAYING, positionMillis(), "Playing");
    }

    private void tick() {
        if (event.state() != PlaybackEvent.State.PLAYING || line == null) return;
        try {
            if (endOfStream) {
                if (line.getLongFramePosition() >= submittedFrames) advance();
            } else if (line.available() >= bytes.length) {
                int count = stream.read(samples, 0, samples.length);
                if (count < 0) endOfStream = true;
                else {
                    for (int sample = 0; sample < count; sample++) {
                        bytes[sample * 2] = (byte) samples[sample];
                        bytes[sample * 2 + 1] = (byte) (samples[sample] >> 8);
                    }
                    int written = line.write(bytes, 0, count * 2);
                    if (written != count * 2) throw new IOException("Incomplete audio output.");
                    submittedFrames += count;
                }
            }
            if (line != null && event.state() == PlaybackEvent.State.PLAYING) {
                publish(PlaybackEvent.State.PLAYING, positionMillis(), "Playing");
            }
        } catch (Exception exception) {
            fail(exception);
        }
    }

    private long positionMillis() {
        return Math.min(event.track().durationMillis(), Math.round(line.getLongFramePosition() * 1000.0 / line.getFormat().getFrameRate()));
    }

    private void advance() {
        if (queue.isEmpty()) return;
        if (index + 1 < queue.size()) {
            index++;
            startTrack();
        } else {
            releaseAudio();
            publish(PlaybackEvent.State.STOPPED, 0, "End of collection");
        }
    }

    private void publish(PlaybackEvent.State state, long position, String message) {
        PlaybackEvent update = new PlaybackEvent(event.track(), position, state, message);
        event = update;
        SwingUtilities.invokeLater(() -> listeners.forEach(listener -> listener.onPlaybackChanged(update)));
    }

    private void fail(Exception exception) {
        releaseAudio();
        String detail = exception.getMessage();
        publish(PlaybackEvent.State.STOPPED, 0, "Cannot play: " + (detail == null ? exception.getClass().getSimpleName() : detail));
    }

    private void releaseAudio() {
        if (line != null) {
            line.stop();
            line.flush();
            line.close();
            line = null;
        }
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException ignored) {
            }
            stream = null;
        }
    }

    @Override
    public synchronized void close() {
        if (closed) return;
        closed = true;
        worker.execute(() -> {
            releaseAudio();
            publish(PlaybackEvent.State.STOPPED, 0, "Closed");
            listeners.clear();
        });
        worker.shutdown();
    }
}
