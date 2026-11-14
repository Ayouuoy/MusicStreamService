package com.musicflow;

import com.musicflow.library.MusicLibrary;
import com.musicflow.model.LyricLine;
import com.musicflow.playback.PlaybackService;
import com.musicflow.playlist.PlaylistManager;
import com.musicflow.ui.MusicFrame;
import java.util.List;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        MusicFacade facade = new MusicFacade(new MusicLibrary(), new PlaylistManager(), new PlaybackService());
        loadSamples(facade);
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
            new MusicFrame(facade).setVisible(true);
        });
    }

    private static void loadSamples(MusicFacade facade) {
        String[] names = {"Morning", "Evening", "Starlight"};
        for (String name : names) {
            try {
                facade.loadSample(Main.class.getResource("/audio/" + name.toLowerCase(java.util.Locale.ROOT) + ".wav"),
                        name, List.of(
                                new LyricLine(0, "[Instrumental demo] " + name),
                                new LyricLine(3000, "A quiet melody begins"),
                                new LyricLine(6000, "Notes drift through the air"),
                                new LyricLine(9000, "Turn on echo to hear the reflection"),
                                new LyricLine(12000, "The final notes fade away")));
            } catch (Exception exception) {
                System.err.println("Cannot load " + name + ": " + exception.getMessage());
            }
        }
    }
}
