package com.musicflow.ui;

import com.musicflow.MusicFacade;
import com.musicflow.model.Track;
import com.musicflow.playback.SequentialStrategy;
import com.musicflow.playback.ShuffleStrategy;
import com.musicflow.playlist.Playlist;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;

public final class MusicFrame extends JFrame {
    private final MusicFacade facade;
    private final DefaultListModel<Track> libraryModel = new DefaultListModel<>();
    private final DefaultListModel<Track> playlistModel = new DefaultListModel<>();
    private final JList<Track> library = new JList<>(libraryModel);
    private final JList<Track> playlistTracks = new JList<>(playlistModel);
    private final JComboBox<Playlist> playlists = new JComboBox<>();
    private final JTextField playlistName = new JTextField(12);
    private final JLabel libraryStats = new JLabel();
    private final JLabel playlistStats = new JLabel("0 tracks | 0:00");
    private final JLabel message = new JLabel("WAV files and optional same-name .lrc lyrics. Double-click a track to play.");
    private final NowPlayingPanel nowPlaying = new NowPlayingPanel();
    private final LyricsPanel lyrics = new LyricsPanel();
    private JList<Track> activeList;

    public MusicFrame(MusicFacade facade) {
        super("MusicFlow");
        this.facade = facade;
        activeList = library;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JPanel collections = new JPanel(new GridLayout(1, 2, 8, 0));
        collections.add(libraryPanel());
        collections.add(playlistPanel());
        JPanel center = new JPanel(new BorderLayout(8, 8));
        center.add(collections, BorderLayout.CENTER);
        lyrics.setPreferredSize(new Dimension(240, 130));
        center.add(lyrics, BorderLayout.SOUTH);
        content.add(center, BorderLayout.CENTER);
        content.add(playerPanel(), BorderLayout.SOUTH);
        content.add(message, BorderLayout.NORTH);
        setContentPane(content);
        configureTrackList(library);
        configureTrackList(playlistTracks);
        facade.subscribe(nowPlaying);
        facade.subscribe(lyrics);
        refreshLibrary();
        for (Playlist playlist : facade.getPlaylists()) playlists.addItem(playlist);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent event) {
                facade.unsubscribe(nowPlaying);
                facade.unsubscribe(lyrics);
                facade.close();
            }
        });
        setMinimumSize(new Dimension(780, 660));
        setSize(900, 720);
        setLocationRelativeTo(null);
    }

    private JPanel libraryPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createTitledBorder("Music library"));
        panel.add(button("Import WAV", this::importAudio), BorderLayout.NORTH);
        panel.add(new JScrollPane(library), BorderLayout.CENTER);
        panel.add(libraryStats, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel playlistPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createTitledBorder("Playlists"));
        JPanel header = new JPanel(new GridLayout(3, 1, 0, 4));
        JPanel create = new JPanel(new BorderLayout(4, 0));
        create.add(playlistName, BorderLayout.CENTER);
        create.add(button("Create", this::createPlaylist), BorderLayout.EAST);
        playlistName.setToolTipText("New playlist name");
        header.add(create);
        header.add(playlists);
        header.add(button("Add selected library tracks", this::addTracks));
        playlists.addActionListener(event -> refreshPlaylist());
        panel.add(header, BorderLayout.NORTH);
        panel.add(new JScrollPane(playlistTracks), BorderLayout.CENTER);
        panel.add(playlistStats, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel playerPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.add(nowPlaying, BorderLayout.NORTH);
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        controls.add(button("Previous", facade::previous));
        controls.add(button("Play", this::playSelection));
        controls.add(button("Pause", facade::pause));
        controls.add(button("Stop", facade::stop));
        controls.add(button("Next", facade::next));
        JComboBox<String> order = new JComboBox<>(new String[]{"Sequential", "Shuffle"});
        order.addActionListener(event -> facade.setStrategy(order.getSelectedIndex() == 0
                ? new SequentialStrategy() : new ShuffleStrategy()));
        controls.add(order);
        JPanel effects = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        JSlider volume = new JSlider(0, 100, 70);
        volume.setPreferredSize(new Dimension(160, 32));
        volume.setToolTipText("Volume");
        volume.addChangeListener(event -> facade.setVolume(volume.getValue() / 100.0));
        JCheckBox echo = new JCheckBox("Echo");
        echo.addActionListener(event -> facade.setEcho(echo.isSelected()));
        effects.add(new JLabel("Volume"));
        effects.add(volume);
        effects.add(echo);
        panel.add(controls, BorderLayout.CENTER);
        panel.add(effects, BorderLayout.SOUTH);
        return panel;
    }

    private JButton button(String text, Runnable action) {
        JButton button = new JButton(text);
        button.addActionListener(event -> {
            try {
                action.run();
            } catch (IllegalArgumentException exception) {
                message.setText(exception.getMessage());
            }
        });
        return button;
    }

    private void configureTrackList(JList<Track> list) {
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        list.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting() && !list.isSelectionEmpty()) activeList = list;
        });
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2 && !list.isSelectionEmpty()) playSelection();
            }
        });
    }

    private void playSelection() {
        Track selected = activeList.getSelectedValue();
        if (selected == null) {
            facade.resume();
            message.setText("Select a track in the library or playlist.");
            return;
        }
        List<Track> tracks = activeList == library ? facade.getTracks() : selectedPlaylist().getTracks();
        facade.play(tracks, selected);
        message.setText(activeList == library ? "Playing from library" : "Playing from playlist");
    }

    private Playlist selectedPlaylist() {
        Playlist selected = (Playlist) playlists.getSelectedItem();
        if (selected == null) throw new IllegalArgumentException("Create or select a playlist first.");
        return selected;
    }

    private void createPlaylist() {
        Playlist created = facade.createPlaylist(playlistName.getText());
        playlists.addItem(created);
        playlists.setSelectedItem(created);
        playlistName.setText("");
        message.setText("Playlist created. Select library tracks and add them.");
    }

    private void addTracks() {
        Playlist updated = facade.addTracks(selectedPlaylist(), library.getSelectedValuesList());
        int index = playlists.getSelectedIndex();
        playlists.removeItemAt(index);
        playlists.insertItemAt(updated, index);
        playlists.setSelectedItem(updated);
        message.setText("Tracks added to " + updated.getName());
    }

    private void refreshLibrary() {
        libraryModel.clear();
        facade.getTracks().forEach(libraryModel::addElement);
        libraryStats.setText(facade.libraryStatistics().toString());
    }

    private void refreshPlaylist() {
        playlistModel.clear();
        Playlist selected = (Playlist) playlists.getSelectedItem();
        if (selected == null) {
            playlistStats.setText("0 tracks | 0:00");
            return;
        }
        selected.getTracks().forEach(playlistModel::addElement);
        playlistStats.setText(facade.statistics(selected).toString());
    }

    private void importAudio() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("WAV audio", "wav"));
        chooser.setMultiSelectionEnabled(true);
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File[] files = chooser.getSelectedFiles();
        message.setText("Importing audio...");
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                int imported = 0;
                String failure = "";
                for (File file : files) {
                    try {
                        facade.importWav(file.toPath());
                        imported++;
                    } catch (Exception exception) {
                        failure = " | " + file.getName() + ": " + exception.getMessage();
                    }
                }
                return "Loaded " + imported + " file(s)" + failure;
            }

            @Override
            protected void done() {
                refreshLibrary();
                try {
                    message.setText(get());
                } catch (Exception exception) {
                    message.setText("Could not import audio.");
                }
            }
        }.execute();
    }
}
