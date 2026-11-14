package com.musicflow;

import com.musicflow.library.MusicLibrary;
import com.musicflow.model.LyricLine;
import com.musicflow.model.Track;
import com.musicflow.playback.PlaybackListener;
import com.musicflow.playback.PlaybackOrderStrategy;
import com.musicflow.playback.PlaybackService;
import com.musicflow.playlist.Playlist;
import com.musicflow.playlist.PlaylistManager;
import com.musicflow.visitor.DurationVisitor;
import com.musicflow.visitor.TrackCountVisitor;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import javax.sound.sampled.UnsupportedAudioFileException;

public final class MusicFacade implements AutoCloseable {
    private final MusicLibrary library;
    private final PlaylistManager playlists;
    private final PlaybackService playback;

    public MusicFacade(MusicLibrary library, PlaylistManager playlists, PlaybackService playback) {
        this.library = library;
        this.playlists = playlists;
        this.playback = playback;
    }

    public List<Track> getTracks() {
        return library.getTracks();
    }

    public Track importWav(Path path) throws IOException, UnsupportedAudioFileException {
        return library.importWav(path);
    }

    public Track loadSample(URL source, String title, List<LyricLine> lyrics)
            throws IOException, UnsupportedAudioFileException {
        return library.load(source, title, "MusicFlow Demo", lyrics);
    }

    public List<Playlist> getPlaylists() {
        return playlists.getPlaylists();
    }

    public Playlist createPlaylist(String name) {
        return playlists.create(name);
    }

    public Playlist addTracks(Playlist playlist, List<Track> tracks) {
        if (tracks.isEmpty()) throw new IllegalArgumentException("Select tracks in the library first.");
        if (!library.getTracks().containsAll(tracks)) throw new IllegalArgumentException("Tracks must belong to the library.");
        return playlists.addTracks(playlist, tracks);
    }

    public void play(List<Track> tracks, Track selected) {
        playback.play(tracks, selected);
    }

    public void resume() {
        playback.resume();
    }

    public void pause() {
        playback.pause();
    }

    public void stop() {
        playback.stop();
    }

    public void next() {
        playback.next();
    }

    public void previous() {
        playback.previous();
    }

    public void setStrategy(PlaybackOrderStrategy strategy) {
        playback.setStrategy(strategy);
    }

    public void setVolume(double volume) {
        playback.setVolume(volume);
    }

    public void setEcho(boolean enabled) {
        playback.setEcho(enabled);
    }

    public void subscribe(PlaybackListener listener) {
        playback.subscribe(listener);
    }

    public void unsubscribe(PlaybackListener listener) {
        playback.unsubscribe(listener);
    }

    public Statistics statistics(Playlist playlist) {
        DurationVisitor duration = new DurationVisitor();
        TrackCountVisitor count = new TrackCountVisitor();
        playlist.accept(duration);
        playlist.accept(count);
        return new Statistics(count.getCount(), duration.getTotalMillis());
    }

    public Statistics libraryStatistics() {
        DurationVisitor duration = new DurationVisitor();
        TrackCountVisitor count = new TrackCountVisitor();
        for (Track track : library.getTracks()) {
            track.accept(duration);
            track.accept(count);
        }
        return new Statistics(count.getCount(), duration.getTotalMillis());
    }

    @Override
    public void close() {
        playback.close();
    }

    public record Statistics(int tracks, long durationMillis) {
        @Override
        public String toString() {
            long seconds = durationMillis / 1000;
            return tracks + " tracks | " + seconds / 60 + ":" + String.format("%02d", seconds % 60);
        }
    }
}
