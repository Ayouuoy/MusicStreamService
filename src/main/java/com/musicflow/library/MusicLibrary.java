package com.musicflow.library;

import com.musicflow.audio.WavAudioStream;
import com.musicflow.model.LyricLine;
import com.musicflow.model.Track;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

public final class MusicLibrary {
    private static final Pattern LRC_TIME = Pattern.compile("\\[(\\d+):(\\d{2})(?:\\.(\\d{1,3}))?]");
    private final List<Track> tracks = new ArrayList<>();

    public synchronized List<Track> getTracks() {
        return List.copyOf(tracks);
    }

    public Track importWav(Path path) throws IOException, UnsupportedAudioFileException {
        if (!path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".wav")) {
            throw new IOException("Select a WAV file.");
        }
        String fileName = path.getFileName().toString();
        String title = fileName.substring(0, fileName.length() - 4);
        Path lyrics = path.resolveSibling(title + ".lrc");
        return load(path.toUri().toURL(), title, "Local file", readLyrics(lyrics));
    }

    public Track load(URL source, String title, String artist, List<LyricLine> lyrics)
            throws IOException, UnsupportedAudioFileException {
        if (source == null) throw new IOException("Audio file was not found.");
        long duration;
        try (AudioInputStream input = AudioSystem.getAudioInputStream(source)) {
            if (!AudioSystem.isConversionSupported(WavAudioStream.PCM, input.getFormat())) {
                throw new IOException("Unsupported PCM conversion for this WAV file.");
            }
            if (input.getFrameLength() < 0 || input.getFormat().getFrameRate() <= 0) {
                throw new IOException("This WAV file has no valid duration.");
            }
            duration = Math.round(input.getFrameLength() * 1000.0 / input.getFormat().getFrameRate());
        }
        Track track = new Track(title, artist, source, duration, lyrics);
        synchronized (this) {
            Track existing = tracks.stream().filter(item -> item.source().toExternalForm()
                    .equals(source.toExternalForm())).findFirst().orElse(null);
            if (existing != null) return existing;
            tracks.add(track);
        }
        return track;
    }

    private List<LyricLine> readLyrics(Path path) throws IOException {
        if (!Files.isRegularFile(path)) return List.of();
        List<LyricLine> lyrics = new ArrayList<>();
        for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            Matcher matcher = LRC_TIME.matcher(line);
            List<Long> times = new ArrayList<>();
            int textStart = 0;
            while (matcher.find()) {
                try {
                    long minutes = Long.parseLong(matcher.group(1));
                    int seconds = Integer.parseInt(matcher.group(2));
                    String fraction = matcher.group(3);
                    int millis = fraction == null ? 0 : Integer.parseInt((fraction + "000").substring(0, 3));
                    if (seconds < 60 && minutes < 1_000_000) times.add(minutes * 60_000 + seconds * 1000L + millis);
                    textStart = matcher.end();
                } catch (NumberFormatException ignored) {
                    textStart = matcher.end();
                }
            }
            String text = line.substring(textStart).trim();
            for (long time : times) lyrics.add(new LyricLine(time, text));
        }
        return lyrics;
    }
}
