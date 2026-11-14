package com.musicflow.ui;

import com.musicflow.playback.PlaybackEvent;
import com.musicflow.playback.PlaybackListener;
import java.awt.BorderLayout;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

public final class NowPlayingPanel extends JPanel implements PlaybackListener {
    private final JLabel title = new JLabel("Select a track");
    private final JLabel status = new JLabel("Ready");
    private final JProgressBar progress = new JProgressBar(0, 1000);

    public NowPlayingPanel() {
        super(new BorderLayout(6, 6));
        setBorder(BorderFactory.createTitledBorder("Now playing"));
        progress.setStringPainted(true);
        progress.setString("0:00 / 0:00");
        add(title, BorderLayout.NORTH);
        add(progress, BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);
    }

    @Override
    public void onPlaybackChanged(PlaybackEvent event) {
        long duration = event.track() == null ? 0 : event.track().durationMillis();
        title.setText(event.track() == null ? "Select a track" : event.track().toString());
        status.setText(event.message());
        progress.setValue(duration == 0 ? 0 : (int) (event.positionMillis() * 1000 / duration));
        progress.setString(time(event.positionMillis()) + " / " + time(duration));
    }

    private String time(long millis) {
        long seconds = millis / 1000;
        return seconds / 60 + ":" + String.format("%02d", seconds % 60);
    }
}
