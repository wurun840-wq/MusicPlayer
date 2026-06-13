package musicplayer.ui;

import musicplayer.exception.DuplicateItemException;
import musicplayer.exception.InvalidMusicDataException;
import musicplayer.model.PlaybackRecord;
import musicplayer.model.Playlist;
import musicplayer.model.Song;
import musicplayer.service.MusicLibrary;
import musicplayer.service.PlayerService;
import musicplayer.storage.FileStorage;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class MusicPlayerApp extends JFrame {
    private static final String DATA_DIR = "data";
    private static final DateTimeFormatter HISTORY_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final FileStorage storage = new FileStorage(DATA_DIR);
    private MusicLibrary library;
    private PlayerService player;

    private final DefaultTableModel songTableModel = new DefaultTableModel(
            new String[]{"Title", "Artist", "Album", "Genre", "Duration", "Favorite", "Plays"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable songTable = new JTable(songTableModel);

    private final DefaultTableModel playlistTableModel = new DefaultTableModel(
            new String[]{"Playlist", "Type", "Songs"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable playlistTable = new JTable(playlistTableModel);

    private final DefaultTableModel playlistSongsModel = new DefaultTableModel(
            new String[]{"Title", "Artist", "Duration"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable playlistSongsTable = new JTable(playlistSongsModel);

    private final JTextField searchField = new JTextField(20);
    private final JComboBox<String> genreFilter = new JComboBox<String>();
    private final JComboBox<String> sortBox = new JComboBox<String>(new String[]{"Original", "Title", "Artist"});
    private final JComboBox<String> playerSongBox = new JComboBox<String>();
    private final JComboBox<String> playerPlaylistBox = new JComboBox<String>();
    private final JTextArea historyArea = new JTextArea();
    private final JTextArea statsArea = new JTextArea();
    private final JLabel currentSongLabel = new JLabel("No song is playing.");
    private final JLabel statusLabel = new JLabel("Ready");

    private List<Song> visibleSongs = new ArrayList<Song>();

    public MusicPlayerApp() {
        loadLibrary();
        configureWindow();
        buildLayout();
        refreshAll();
    }

    private void loadLibrary() {
        try {
            library = storage.load();
        } catch (IOException e) {
            library = new MusicLibrary();
            showError("Could not load saved data: " + e.getMessage());
        }
        player = new PlayerService(library);
    }

    private void configureWindow() {
        setTitle("Music Playlist Manager with Player - Multi Page");
        setSize(980, 660);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveAndExit();
            }
        });
    }

    private void buildLayout() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Songs", buildSongsPage());
        tabs.addTab("Playlists", buildPlaylistsPage());
        tabs.addTab("Player", buildPlayerPage());
        tabs.addTab("History & Stats", buildHistoryStatsPage());

        JPanel main = new JPanel(new BorderLayout(8, 8));
        main.setBorder(BorderFactory.createEmptyBorder(10, 10, 8, 10));
        main.add(tabs, BorderLayout.CENTER);
        main.add(statusLabel, BorderLayout.SOUTH);
        setContentPane(main);
    }

    private JPanel buildSongsPage() {
        JPanel page = new JPanel(new BorderLayout(8, 8));
        page.add(buildSearchBar(), BorderLayout.NORTH);

        songTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        page.add(new JScrollPane(songTable), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        JButton add = new JButton("Add Song");
        JButton edit = new JButton("Edit Song");
        JButton delete = new JButton("Delete Song");
        JButton favorite = new JButton("Favorite");
        JButton addToPlaylist = new JButton("Add to Playlist");
        add.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addSong();
            }
        });
        edit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                editSong();
            }
        });
        delete.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteSong();
            }
        });
        favorite.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleFavorite();
            }
        });
        addToPlaylist.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addSelectedSongToPlaylist();
            }
        });
        buttons.add(add);
        buttons.add(edit);
        buttons.add(delete);
        buttons.add(favorite);
        buttons.add(addToPlaylist);
        page.add(buttons, BorderLayout.SOUTH);
        return page;
    }

    private JPanel buildSearchBar() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        panel.setBorder(BorderFactory.createTitledBorder("Search and Filter"));
        panel.add(new JLabel("Keyword"));
        panel.add(searchField);
        panel.add(new JLabel("Genre"));
        panel.add(genreFilter);
        panel.add(new JLabel("Sort"));
        panel.add(sortBox);

        JButton apply = new JButton("Apply");
        JButton reset = new JButton("Reset");
        apply.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refreshSongs();
            }
        });
        reset.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                searchField.setText("");
                genreFilter.setSelectedItem("All");
                sortBox.setSelectedItem("Original");
                refreshSongs();
            }
        });
        panel.add(apply);
        panel.add(reset);
        return panel;
    }

    private JPanel buildPlaylistsPage() {
        JPanel page = new JPanel(new BorderLayout(8, 8));
        playlistTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        playlistTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                refreshPlaylistSongs();
            }
        });

        JPanel tables = new JPanel(new GridLayout(2, 1, 8, 8));
        tables.add(wrap("Playlists", playlistTable));
        tables.add(wrap("Songs in Selected Playlist", playlistSongsTable));
        page.add(tables, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        JButton create = new JButton("New Playlist");
        JButton rename = new JButton("Rename");
        JButton delete = new JButton("Delete");
        JButton addSong = new JButton("Add Selected Song");
        JButton removeSong = new JButton("Remove Playlist Song");
        create.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                createPlaylist();
            }
        });
        rename.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                renamePlaylist();
            }
        });
        delete.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deletePlaylist();
            }
        });
        addSong.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addSelectedSongToPlaylist();
            }
        });
        removeSong.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                removeSongFromPlaylist();
            }
        });
        buttons.add(create);
        buttons.add(rename);
        buttons.add(delete);
        buttons.add(addSong);
        buttons.add(removeSong);
        page.add(buttons, BorderLayout.SOUTH);
        return page;
    }

    private JPanel buildPlayerPage() {
        JPanel page = new JPanel(new BorderLayout(8, 8));

        JPanel selectors = new JPanel(new GridLayout(2, 1, 8, 8));
        selectors.setBorder(BorderFactory.createTitledBorder("Choose Music to Play"));
        selectors.add(labeledCombo("Song", playerSongBox));
        selectors.add(labeledCombo("Playlist", playerPlaylistBox));
        page.add(selectors, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridLayout(4, 1, 8, 8));
        center.setBorder(BorderFactory.createTitledBorder("Playback Controls"));
        currentSongLabel.setHorizontalAlignment(JLabel.CENTER);
        center.add(currentSongLabel);

        JPanel rowOne = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 8));
        JButton playSong = new JButton("Play Song");
        JButton playPlaylist = new JButton("Play Playlist");
        JButton pause = new JButton("Pause");
        JButton resume = new JButton("Resume");
        playSong.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                playSongFromCombo();
            }
        });
        playPlaylist.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                playPlaylistFromCombo();
            }
        });
        pause.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                player.pause();
                setStatus("Paused.");
            }
        });
        resume.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                player.resume();
                setStatus("Resumed.");
            }
        });
        rowOne.add(playSong);
        rowOne.add(playPlaylist);
        rowOne.add(pause);
        rowOne.add(resume);
        center.add(rowOne);

        JPanel rowTwo = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 8));
        JButton stop = new JButton("Stop");
        JButton skip = new JButton("Skip");
        JButton shuffle = new JButton("Shuffle Off");
        JButton repeat = new JButton("Repeat Off");
        stop.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                player.stop();
                setStatus("Stopped.");
                currentSongLabel.setText("No song is playing.");
            }
        });
        skip.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                skipSong();
            }
        });
        shuffle.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                player.toggleShuffle();
                shuffle.setText(player.isShuffle() ? "Shuffle On" : "Shuffle Off");
            }
        });
        repeat.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                player.toggleRepeat();
                repeat.setText(player.isRepeat() ? "Repeat On" : "Repeat Off");
            }
        });
        rowTwo.add(stop);
        rowTwo.add(skip);
        rowTwo.add(shuffle);
        rowTwo.add(repeat);
        center.add(rowTwo);

        JPanel rowThree = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 8));
        JButton save = new JButton("Save Now");
        JButton refresh = new JButton("Refresh Lists");
        save.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveData();
            }
        });
        refresh.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refreshAll();
            }
        });
        rowThree.add(save);
        rowThree.add(refresh);
        center.add(rowThree);

        page.add(center, BorderLayout.CENTER);
        return page;
    }

    private JPanel buildHistoryStatsPage() {
        JPanel page = new JPanel(new GridLayout(1, 2, 8, 8));
        historyArea.setEditable(false);
        statsArea.setEditable(false);
        page.add(wrap("Playback History", historyArea));
        page.add(wrap("Listening Statistics", statsArea));
        return page;
    }

    private JScrollPane wrap(String title, JTable table) {
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createTitledBorder(title));
        return scrollPane;
    }

    private JScrollPane wrap(String title, JTextArea area) {
        JScrollPane scrollPane = new JScrollPane(area);
        scrollPane.setBorder(BorderFactory.createTitledBorder(title));
        return scrollPane;
    }

    private JPanel labeledCombo(String label, JComboBox<String> comboBox) {
        JPanel panel = new JPanel(new BorderLayout(8, 0));
        panel.add(new JLabel(label), BorderLayout.WEST);
        panel.add(comboBox, BorderLayout.CENTER);
        return panel;
    }

    private void addSong() {
        SongForm form = new SongForm(null);
        if (form.showDialog("Add Song")) {
            try {
                library.addSong(form.title(), form.artist(), form.album(), form.genre(),
                        form.durationSeconds(), form.filePath());
                refreshAll();
                setStatus("Song added.");
            } catch (DuplicateItemException | InvalidMusicDataException ex) {
                showError(ex.getMessage());
            }
        }
    }

    private void editSong() {
        Song song = getSelectedSong();
        if (song == null) {
            showError("Please select a song on the Songs page first.");
            return;
        }
        SongForm form = new SongForm(song);
        if (form.showDialog("Edit Song")) {
            try {
                library.updateSong(song.getId(), form.title(), form.artist(), form.album(),
                        form.genre(), form.durationSeconds(), form.filePath());
                refreshAll();
                setStatus("Song updated.");
            } catch (InvalidMusicDataException ex) {
                showError(ex.getMessage());
            }
        }
    }

    private void deleteSong() {
        Song song = getSelectedSong();
        if (song == null) {
            showError("Please select a song on the Songs page first.");
            return;
        }
        if (confirm("Delete selected song?")) {
            library.deleteSong(song.getId());
            refreshAll();
            setStatus("Song deleted.");
        }
    }

    private void toggleFavorite() {
        Song song = getSelectedSong();
        if (song == null) {
            showError("Please select a song on the Songs page first.");
            return;
        }
        try {
            library.toggleFavorite(song.getId());
            refreshAll();
            setStatus("Favorite status changed.");
        } catch (InvalidMusicDataException ex) {
            showError(ex.getMessage());
        }
    }

    private void createPlaylist() {
        JTextField nameField = new JTextField(24);
        JCheckBox favoriteType = new JCheckBox("Create as favorite playlist type");
        JPanel form = new JPanel(new GridLayout(2, 1, 4, 4));
        form.add(labeledText("Name", nameField));
        form.add(favoriteType);
        int result = JOptionPane.showConfirmDialog(this, form, "New Playlist",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            try {
                library.createPlaylist(nameField.getText(), favoriteType.isSelected());
                refreshAll();
                setStatus("Playlist created.");
            } catch (DuplicateItemException | InvalidMusicDataException ex) {
                showError(ex.getMessage());
            }
        }
    }

    private void renamePlaylist() {
        Playlist playlist = getSelectedPlaylist();
        if (playlist == null) {
            showError("Please select a playlist first.");
            return;
        }
        String name = JOptionPane.showInputDialog(this, "New playlist name:", playlist.getName());
        if (name != null) {
            try {
                library.renamePlaylist(playlist.getId(), name);
                refreshAll();
                setStatus("Playlist renamed.");
            } catch (InvalidMusicDataException ex) {
                showError(ex.getMessage());
            }
        }
    }

    private void deletePlaylist() {
        Playlist playlist = getSelectedPlaylist();
        if (playlist == null) {
            showError("Please select a playlist first.");
            return;
        }
        if (confirm("Delete selected playlist?")) {
            library.deletePlaylist(playlist.getId());
            refreshAll();
            setStatus("Playlist deleted.");
        }
    }

    private void addSelectedSongToPlaylist() {
        Song song = getSelectedSong();
        Playlist playlist = getSelectedPlaylist();
        if (song == null || playlist == null) {
            showError("Select a song on the Songs page and a playlist on the Playlists page.");
            return;
        }
        try {
            library.addSongToPlaylist(playlist.getId(), song.getId());
            refreshAll();
            setStatus("Song added to playlist.");
        } catch (DuplicateItemException | InvalidMusicDataException ex) {
            showError(ex.getMessage());
        }
    }

    private void removeSongFromPlaylist() {
        Playlist playlist = getSelectedPlaylist();
        int row = playlistSongsTable.getSelectedRow();
        if (playlist == null || row < 0) {
            showError("Please select a song inside the selected playlist.");
            return;
        }
        List<Song> songs = library.getSongsInPlaylist(playlist);
        try {
            library.removeSongFromPlaylist(playlist.getId(), songs.get(row).getId());
            refreshAll();
            setStatus("Song removed from playlist.");
        } catch (InvalidMusicDataException ex) {
            showError(ex.getMessage());
        }
    }

    private void playSongFromCombo() {
        int index = playerSongBox.getSelectedIndex();
        if (index < 0 || index >= library.getSongs().size()) {
            showError("Please add a song first.");
            return;
        }
        playSong(library.getSongs().get(index));
    }

    private void playPlaylistFromCombo() {
        int index = playerPlaylistBox.getSelectedIndex();
        if (index < 0 || index >= library.getPlaylists().size()) {
            showError("Please create a playlist first.");
            return;
        }
        Playlist playlist = library.getPlaylists().get(index);
        try {
            playlist.play(player);
            refreshAll();
            Song current = player.getCurrentSong();
            currentSongLabel.setText(current == null ? "Playing playlist: " + playlist.getName()
                    : "Playing: " + current.getDisplayText());
            setStatus("Playing playlist: " + playlist.getName());
        } catch (InvalidMusicDataException ex) {
            showError(ex.getMessage());
        }
    }

    private void playSong(Song song) {
        try {
            song.play(player);
            refreshAll();
            currentSongLabel.setText("Playing: " + song.getDisplayText());
            setStatus("Playing: " + song.getDisplayText());
        } catch (InvalidMusicDataException ex) {
            showError(ex.getMessage());
        }
    }

    private void skipSong() {
        try {
            player.next();
            refreshAll();
            Song current = player.getCurrentSong();
            currentSongLabel.setText(current == null ? "No next song." : "Playing: " + current.getDisplayText());
            setStatus(current == null ? "No next song." : "Playing: " + current.getDisplayText());
        } catch (InvalidMusicDataException ex) {
            showError(ex.getMessage());
        }
    }

    private void refreshAll() {
        refreshGenres();
        refreshSongs();
        refreshPlaylists();
        refreshPlaylistSongs();
        refreshPlayerChoices();
        refreshHistoryAndStats();
    }

    private void refreshGenres() {
        Object selected = genreFilter.getSelectedItem();
        genreFilter.removeAllItems();
        for (String genre : library.getGenres()) {
            genreFilter.addItem(genre);
        }
        genreFilter.setSelectedItem(selected == null ? "All" : selected);
    }

    private void refreshSongs() {
        String sort = (String) sortBox.getSelectedItem();
        List<Song> filtered = library.searchSongs(searchField.getText(), (String) genreFilter.getSelectedItem());
        if ("Title".equals(sort)) {
            List<Song> sorted = library.sortSongsByTitle();
            visibleSongs = keepFilteredSongs(sorted, filtered);
        } else if ("Artist".equals(sort)) {
            List<Song> sorted = library.sortSongsByArtist();
            visibleSongs = keepFilteredSongs(sorted, filtered);
        } else {
            visibleSongs = filtered;
        }

        songTableModel.setRowCount(0);
        for (Song song : visibleSongs) {
            songTableModel.addRow(new Object[]{
                    song.getTitle(),
                    song.getArtist(),
                    song.getAlbum(),
                    song.getGenre(),
                    song.getDurationText(),
                    song.isFavorite() ? "Yes" : "No",
                    song.getPlayCount()
            });
        }
    }

    private List<Song> keepFilteredSongs(List<Song> sorted, List<Song> filtered) {
        List<Song> result = new ArrayList<Song>();
        for (int i = 0; i < sorted.size(); i++) {
            Song song = sorted.get(i);
            if (filtered.contains(song)) {
                result.add(song);
            }
        }
        return result;
    }

    private void refreshPlaylists() {
        playlistTableModel.setRowCount(0);
        for (Playlist playlist : library.getPlaylists()) {
            playlistTableModel.addRow(new Object[]{
                    playlist.getName(),
                    playlist.getType(),
                    playlist.getSongCount()
            });
        }
    }

    private void refreshPlaylistSongs() {
        playlistSongsModel.setRowCount(0);
        Playlist playlist = getSelectedPlaylist();
        if (playlist == null) {
            return;
        }
        for (Song song : library.getSongsInPlaylist(playlist)) {
            playlistSongsModel.addRow(new Object[]{song.getTitle(), song.getArtist(), song.getDurationText()});
        }
    }

    private void refreshPlayerChoices() {
        Object selectedSong = playerSongBox.getSelectedItem();
        Object selectedPlaylist = playerPlaylistBox.getSelectedItem();
        playerSongBox.removeAllItems();
        for (Song song : library.getSongs()) {
            playerSongBox.addItem(song.getDisplayText());
        }
        playerPlaylistBox.removeAllItems();
        for (Playlist playlist : library.getPlaylists()) {
            playerPlaylistBox.addItem(playlist.getDisplayText());
        }
        if (selectedSong != null) {
            playerSongBox.setSelectedItem(selectedSong);
        }
        if (selectedPlaylist != null) {
            playerPlaylistBox.setSelectedItem(selectedPlaylist);
        }
    }

    private void refreshHistoryAndStats() {
        StringBuilder historyText = new StringBuilder();
        for (PlaybackRecord record : library.getHistory()) {
            historyText.append(record.getPlayedAt().format(HISTORY_TIME))
                    .append("  ")
                    .append(record.getSongTitle())
                    .append(" - ")
                    .append(record.getArtist())
                    .append("\n");
        }
        historyArea.setText(historyText.toString());
        statsArea.setText(library.getStatisticsReport());
    }

    private Song getSelectedSong() {
        int row = songTable.getSelectedRow();
        if (row < 0 || row >= visibleSongs.size()) {
            return null;
        }
        return visibleSongs.get(row);
    }

    private Playlist getSelectedPlaylist() {
        int row = playlistTable.getSelectedRow();
        if (row < 0 || row >= library.getPlaylists().size()) {
            return null;
        }
        return library.getPlaylists().get(row);
    }

    private JPanel labeledText(String label, JTextField field) {
        JPanel panel = new JPanel(new BorderLayout(6, 0));
        panel.add(new JLabel(label), BorderLayout.WEST);
        panel.add(field, BorderLayout.CENTER);
        return panel;
    }

    private boolean confirm(String message) {
        return JOptionPane.showConfirmDialog(this, message, "Confirm",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    private void saveData() {
        try {
            storage.save(library);
            setStatus("Data saved.");
        } catch (IOException ex) {
            showError("Could not save data: " + ex.getMessage());
        }
    }

    private void saveAndExit() {
        saveData();
        dispose();
        System.exit(0);
    }

    private void setStatus(String message) {
        statusLabel.setText(message);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Message", JOptionPane.INFORMATION_MESSAGE);
        setStatus(message);
    }

    private class SongForm {
        private final JTextField title = new JTextField(28);
        private final JTextField artist = new JTextField(28);
        private final JTextField album = new JTextField(28);
        private final JTextField genre = new JTextField(28);
        private final JTextField duration = new JTextField(28);
        private final JTextField path = new JTextField(28);

        SongForm(Song song) {
            if (song != null) {
                title.setText(song.getTitle());
                artist.setText(song.getArtist());
                album.setText(song.getAlbum());
                genre.setText(song.getGenre());
                duration.setText(String.valueOf(song.getDurationSeconds()));
                path.setText(song.getFilePath());
            }
        }

        boolean showDialog(String titleText) {
            JPanel form = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(6, 6, 6, 6);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1;
            addRow(form, gbc, 0, "Title", title);
            addRow(form, gbc, 1, "Artist", artist);
            addRow(form, gbc, 2, "Album", album);
            addRow(form, gbc, 3, "Genre", genre);
            addRow(form, gbc, 4, "Duration seconds", duration);
            addRow(form, gbc, 5, "File path", path);
            JButton browse = new JButton("Browse");
            browse.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    chooseFile();
                }
            });
            gbc.gridx = 2;
            gbc.gridy = 5;
            gbc.gridwidth = 1;
            gbc.weightx = 0;
            form.add(browse, gbc);
            return JOptionPane.showConfirmDialog(MusicPlayerApp.this, form, titleText,
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION;
        }

        String title() {
            return title.getText().trim();
        }

        String artist() {
            return artist.getText().trim();
        }

        String album() {
            return album.getText().trim();
        }

        String genre() {
            return genre.getText().trim().isEmpty() ? "Unknown" : genre.getText().trim();
        }

        int durationSeconds() throws InvalidMusicDataException {
            try {
                return Integer.parseInt(duration.getText().trim());
            } catch (NumberFormatException e) {
                throw new InvalidMusicDataException("Duration must be a whole number.");
            }
        }

        String filePath() {
            return path.getText().trim();
        }

        private void chooseFile() {
            JFileChooser chooser = new JFileChooser();
            if (chooser.showOpenDialog(MusicPlayerApp.this) == JFileChooser.APPROVE_OPTION) {
                path.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        }

        private void addRow(JPanel form, GridBagConstraints gbc, int row, String label, JTextField field) {
            gbc.gridx = 0;
            gbc.gridy = row;
            gbc.weightx = 0;
            gbc.gridwidth = 1;
            form.add(new JLabel(label), gbc);
            gbc.gridx = 1;
            gbc.weightx = 1;
            gbc.gridwidth = row == 5 ? 1 : 2;
            form.add(field, gbc);
        }
    }
}
