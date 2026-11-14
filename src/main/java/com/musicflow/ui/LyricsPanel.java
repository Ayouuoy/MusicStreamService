package com.musicflow.ui;

import com.musicflow.model.Track;
import com.musicflow.playback.PlaybackEvent;
import com.musicflow.playback.PlaybackListener;
import java.awt.BorderLayout;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public final class LyricsPanel extends JPanel implements PlaybackListener {
    private final DefaultListModel<String> lines = new DefaultListModel<>();
    private final JList<String> lyrics = new JList<>(lines);
    private Track currentTrack;

    public LyricsPanel() {
        super(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Synchronized lyrics"));
        lyrics.setFocusable(false);
        add(new JScrollPane(lyrics), BorderLayout.CENTER);
    }

    @Override
    public void onPlaybackChanged(PlaybackEvent event) {
        Track track = event.track();
        if (track != currentTrack) {
            currentTrack = track;
            lines.clear();
            if (track == null || track.lyrics().isEmpty()) lines.addElement("No timed lyrics for this track");
            else track.lyrics().forEach(line -> lines.addElement(line.text()));
        }
        if (track == null || track.lyrics().isEmpty()) return;
        int selected = -1;
        for (int index = 0; index < track.lyrics().size(); index++) {
            if (track.lyrics().get(index).timeMillis() > event.positionMillis()) break;
            selected = index;
        }
        lyrics.setSelectedIndex(selected);
        if (selected >= 0) lyrics.ensureIndexIsVisible(selected);
    }
}
