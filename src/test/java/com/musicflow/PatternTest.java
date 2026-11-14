package com.musicflow;

import com.musicflow.library.MusicLibrary;
import com.musicflow.model.LyricLine;
import com.musicflow.model.Track;
import com.musicflow.playback.PlaybackService;
import com.musicflow.playback.SequentialStrategy;
import com.musicflow.playback.ShuffleStrategy;
import com.musicflow.playlist.Playlist;
import com.musicflow.playlist.PlaylistBuilder;
import com.musicflow.playlist.PlaylistManager;
import com.musicflow.visitor.DurationVisitor;
import com.musicflow.visitor.LibraryVisitor;
import com.musicflow.visitor.TrackCountVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class PatternTest {
    @TempDir
    Path temporary;

    private Track track(String title, long duration) {
        return new Track(title, "Demo", getClass().getResource("/audio/morning.wav"), duration, List.of());
    }

    @Test
    void builderValidatesNameAndMakesIndependentImmutablePlaylists() {
        assertThrows(IllegalArgumentException.class, () -> new PlaylistBuilder().build());
        assertThrows(IllegalArgumentException.class, () -> new PlaylistBuilder().setName(" ").build());
        Track first = track("One", 1000);
        PlaylistBuilder builder = new PlaylistBuilder().setName(" Favorites ").addTrack(first).addTrack(first);
        Playlist playlist = builder.build();
        builder.addTrack(track("Two", 2000));
        assertEquals("Favorites", playlist.getName());
        assertEquals(List.of(first), playlist.getTracks());
        assertThrows(UnsupportedOperationException.class, () -> playlist.getTracks().clear());
        assertThrows(NullPointerException.class, () -> builder.addTrack(null));
    }

    @Test
    void facadeImportsBuildsAndUpdatesPlaylistUsingVisitors() throws Exception {
        try (MusicFacade facade = new MusicFacade(new MusicLibrary(), new PlaylistManager(), new PlaybackService())) {
            Track first = facade.loadSample(getClass().getResource("/audio/morning.wav"), "Morning", List.of());
            Track second = facade.loadSample(getClass().getResource("/audio/evening.wav"), "Evening", List.of());
            Playlist empty = facade.createPlaylist("Favorites");
            assertEquals(new MusicFacade.Statistics(0, 0), facade.statistics(empty));
            Playlist updated = facade.addTracks(empty, List.of(first, second, first));
            assertEquals(List.of(first, second), updated.getTracks());
            assertEquals(List.of(updated), facade.getPlaylists());
            assertEquals(new MusicFacade.Statistics(2, 30000), facade.statistics(updated));
            assertEquals(new MusicFacade.Statistics(2, 30000), facade.libraryStatistics());
            assertThrows(IllegalArgumentException.class, () -> facade.createPlaylist("FAVORITES"));
            assertThrows(IllegalArgumentException.class, () -> facade.addTracks(updated, List.of()));
            assertThrows(IllegalArgumentException.class, () -> facade.addTracks(updated, List.of(track("Foreign", 1))));
            assertThrows(IllegalArgumentException.class, () -> facade.addTracks(empty, List.of(first)));
        }
    }

    @Test
    void visitorUsesDoubleDispatchForTracksAndPlaylists() {
        Track first = track("One", 1200);
        Track second = track("Two", 2300);
        Playlist playlist = new PlaylistBuilder().setName("Test").addTrack(first).addTrack(second).build();
        List<String> visits = new ArrayList<>();
        LibraryVisitor visitor = new LibraryVisitor() {
            @Override
            public void visit(Track track) {
                visits.add("track:" + track.title());
            }
            @Override
            public void visit(Playlist playlist) {
                visits.add("playlist:" + playlist.getName());
            }
        };
        first.accept(visitor);
        playlist.accept(visitor);
        assertEquals(List.of("track:One", "playlist:Test"), visits);
        DurationVisitor duration = new DurationVisitor();
        TrackCountVisitor count = new TrackCountVisitor();
        playlist.accept(duration);
        playlist.accept(count);
        assertEquals(3500, duration.getTotalMillis());
        assertEquals(2, count.getCount());
        first.accept(duration);
        first.accept(count);
        assertEquals(4700, duration.getTotalMillis());
        assertEquals(3, count.getCount());
    }

    @Test
    void strategiesPreserveInputAndShuffleProducesPermutation() {
        List<Track> tracks = new ArrayList<>();
        for (int i = 0; i < 8; i++) tracks.add(track("Track " + i, 1000));
        List<Track> original = List.copyOf(tracks);
        assertEquals(original, new SequentialStrategy().order(tracks));
        List<Track> shuffled = new ShuffleStrategy(new Random(42)).order(tracks);
        assertEquals(original, tracks);
        assertEquals(new HashSet<>(original), new HashSet<>(shuffled));
        assertEquals(original.size(), shuffled.size());
        assertNotEquals(original, shuffled);
        assertEquals(List.of(), new ShuffleStrategy().order(List.of()));
        assertEquals(List.of(tracks.get(0)), new ShuffleStrategy().order(List.of(tracks.get(0))));
    }

    @Test
    void libraryLoadsTimedLyricsDeduplicatesAndReportsMissingFiles() throws Exception {
        Path wav = temporary.resolve("Demo.wav");
        try (var source = getClass().getResourceAsStream("/audio/morning.wav")) {
            Files.copy(source, wav);
        }
        Files.writeString(temporary.resolve("Demo.lrc"),
                "[ar:Demo]\n[00:03.50]Later\n[00:00.000][00:01.2]Start\n[00:99.0]Invalid\n");
        MusicLibrary library = new MusicLibrary();
        Track loaded = library.importWav(wav);
        assertEquals(15000, loaded.durationMillis());
        assertEquals(List.of(new LyricLine(0, "Start"), new LyricLine(1200, "Start"),
                new LyricLine(3500, "Later")), loaded.lyrics());
        assertSame(loaded, library.importWav(wav));
        assertEquals(1, library.getTracks().size());
        assertThrows(Exception.class, () -> library.importWav(temporary.resolve("Missing.wav")));
        assertThrows(Exception.class, () -> library.importWav(temporary.resolve("Wrong.mp3")));
        Files.writeString(temporary.resolve("Broken.wav"), "broken");
        assertThrows(Exception.class, () -> library.importWav(temporary.resolve("Broken.wav")));
    }
}
