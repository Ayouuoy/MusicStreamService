package com.musicflow.playback;

import com.musicflow.audio.WavAudioStream;
import com.musicflow.model.LyricLine;
import com.musicflow.model.Track;
import com.musicflow.ui.LyricsPanel;
import com.musicflow.ui.NowPlayingPanel;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class PlaybackTest {
    @TempDir
    Path temporary;

    private static final class Output {
        private final AtomicLong frames = new AtomicLong();
        private final AtomicInteger bytesWritten = new AtomicInteger();
        private final boolean consume;
        private AudioFormat format;

        Output(boolean consume) {
            this.consume = consume;
        }

        SourceDataLine line(AudioFormat requested) {
            format = requested;
            frames.set(0);
            return (SourceDataLine) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{SourceDataLine.class}, (proxy, method, args) -> {
                        return switch (method.getName()) {
                            case "getFormat" -> format;
                            case "available", "getBufferSize" -> 8820;
                            case "getLongFramePosition" -> frames.get();
                            case "getFramePosition" -> (int) frames.get();
                            case "write" -> {
                                int length = (int) args[2];
                                bytesWritten.addAndGet(length);
                                if (consume) frames.addAndGet(length / 2);
                                yield length;
                            }
                            case "isOpen", "isRunning", "isActive" -> true;
                            case "toString" -> "Test audio output";
                            default -> null;
                        };
                    });
        }
    }

    private Track track(String title) {
        return new Track(title, "Demo", getClass().getResource("/audio/morning.wav"), 15000,
                List.of(new LyricLine(0, "First"), new LyricLine(3000, "Second")));
    }

    private PlaybackEvent await(BlockingQueue<PlaybackEvent> events, Predicate<PlaybackEvent> condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            PlaybackEvent event = events.poll(100, TimeUnit.MILLISECONDS);
            if (event != null && condition.test(event)) return event;
        }
        throw new AssertionError("Expected playback event was not received.");
    }

    @Test
    void observerNotifiesOnEdtSynchronizesLyricsAndUnsubscribes() throws Exception {
        Output output = new Output(false);
        BlockingQueue<PlaybackEvent> events = new LinkedBlockingQueue<>();
        AtomicBoolean allOnEdt = new AtomicBoolean(true);
        AtomicInteger calls = new AtomicInteger();
        PlaybackListener counter = event -> {
            allOnEdt.compareAndSet(true, SwingUtilities.isEventDispatchThread());
            calls.incrementAndGet();
        };
        LyricsPanel[] lyrics = new LyricsPanel[1];
        NowPlayingPanel[] nowPlaying = new NowPlayingPanel[1];
        SwingUtilities.invokeAndWait(() -> {
            lyrics[0] = new LyricsPanel();
            nowPlaying[0] = new NowPlayingPanel();
        });
        try (PlaybackService service = new PlaybackService(output::line)) {
            service.subscribe(counter);
            service.subscribe(lyrics[0]);
            service.subscribe(nowPlaying[0]);
            service.subscribe(events::add);
            Track track = track("One");
            service.play(List.of(track), track);
            await(events, event -> event.state() == PlaybackEvent.State.PLAYING);
            output.frames.set(44100 * 4);
            await(events, event -> event.positionMillis() == 4000);
            SwingUtilities.invokeAndWait(() -> {
                JList<?> lines = (JList<?>) ((JScrollPane) lyrics[0].getComponent(0)).getViewport().getView();
                assertEquals("Second", lines.getSelectedValue());
                service.unsubscribe(counter);
            });
            int before = calls.get();
            service.pause();
            PlaybackEvent paused = await(events, event -> event.state() == PlaybackEvent.State.PAUSED);
            assertEquals(4000, paused.positionMillis());
            service.resume();
            await(events, event -> event.state() == PlaybackEvent.State.PLAYING);
            service.stop();
            await(events, event -> event.message().equals("Stopped"));
            SwingUtilities.invokeAndWait(() -> {});
            assertEquals(before, calls.get());
            assertTrue(allOnEdt.get());
            assertTrue(output.bytesWritten.get() > 0);
            service.unsubscribe(lyrics[0]);
            service.unsubscribe(nowPlaying[0]);
        }
    }

    @Test
    void nextPreviousAndDynamicStrategiesControlActualCurrentTrack() throws Exception {
        Output output = new Output(false);
        BlockingQueue<PlaybackEvent> events = new LinkedBlockingQueue<>();
        List<Track> tracks = List.of(track("A"), track("B"), track("C"));
        try (PlaybackService service = new PlaybackService(output::line)) {
            service.subscribe(events::add);
            service.play(tracks, tracks.get(1));
            await(events, event -> event.state() == PlaybackEvent.State.PLAYING && event.track().equals(tracks.get(1)));
            List<Track> shuffled = new ShuffleStrategy(new Random(42)).order(tracks);
            service.setStrategy(new ShuffleStrategy(new Random(42)));
            service.next();
            Track expected = shuffled.get(shuffled.indexOf(tracks.get(1)) + 1);
            await(events, event -> event.state() == PlaybackEvent.State.PLAYING && event.track().equals(expected));
            assertNotEquals(tracks.get(2), expected);
            service.setStrategy(new SequentialStrategy());
            service.next();
            await(events, event -> event.state() == PlaybackEvent.State.PLAYING && event.track().equals(tracks.get(1)));
            service.previous();
            await(events, event -> event.state() == PlaybackEvent.State.PLAYING && event.track().equals(tracks.get(0)));
            service.previous();
            await(events, event -> event.positionMillis() == 0 && event.track().equals(tracks.get(0)));
        }
    }

    @Test
    void naturalEndAdvancesQueueAndStopsAtLastTrack() throws Exception {
        Path wav = temporary.resolve("short.wav");
        try (AudioInputStream input = new AudioInputStream(new ByteArrayInputStream(new byte[1764]), WavAudioStream.PCM, 882)) {
            AudioSystem.write(input, AudioFileFormat.Type.WAVE, wav.toFile());
        }
        URL source = wav.toUri().toURL();
        List<Track> tracks = List.of(new Track("A", "Demo", source, 20, List.of()),
                new Track("B", "Demo", source, 20, List.of()));
        Output output = new Output(true);
        BlockingQueue<PlaybackEvent> events = new LinkedBlockingQueue<>();
        try (PlaybackService service = new PlaybackService(output::line)) {
            service.subscribe(events::add);
            service.play(tracks, tracks.get(0));
            await(events, event -> event.state() == PlaybackEvent.State.PLAYING && event.track().equals(tracks.get(1)));
            PlaybackEvent end = await(events, event -> event.message().equals("End of collection"));
            assertEquals(PlaybackEvent.State.STOPPED, end.state());
            assertEquals(tracks.get(1), end.track());
            assertEquals(3528, output.bytesWritten.get());
        }
    }

    @Test
    void deletedFileAndUnavailableAudioDeviceProduceRecoverableEvents() throws Exception {
        BlockingQueue<PlaybackEvent> events = new LinkedBlockingQueue<>();
        Track missing = new Track("Missing", "Demo", temporary.resolve("missing.wav").toUri().toURL(), 1000, List.of());
        Output output = new Output(false);
        try (PlaybackService service = new PlaybackService(output::line)) {
            service.subscribe(events::add);
            service.play(List.of(missing), missing);
            await(events, event -> event.message().startsWith("Cannot play:"));
            Track valid = track("Valid");
            service.play(List.of(valid), valid);
            await(events, event -> event.state() == PlaybackEvent.State.PLAYING && event.track().equals(valid));
            assertThrows(IllegalArgumentException.class, () -> service.play(List.of(), valid));
        }
        try (PlaybackService service = new PlaybackService(format -> {
            throw new IllegalStateException("No audio output is available.");
        })) {
            events.clear();
            service.subscribe(events::add);
            Track valid = track("Valid");
            service.play(List.of(valid), valid);
            PlaybackEvent failure = await(events, event -> event.message().startsWith("Cannot play:"));
            assertTrue(failure.message().contains("No audio output"));
            assertEquals(PlaybackEvent.State.STOPPED, failure.state());
        }
    }
}
