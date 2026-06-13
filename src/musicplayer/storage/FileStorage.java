package musicplayer.storage;

import musicplayer.exception.DuplicateItemException;
import musicplayer.model.CsvConvertible;
import musicplayer.model.FavoritePlaylist;
import musicplayer.model.LibraryItem;
import musicplayer.model.PlaybackRecord;
import musicplayer.model.Playlist;
import musicplayer.model.Song;
import musicplayer.service.MusicLibrary;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

public class FileStorage {
    private final File dataDirectory;
    private final File songsFile;
    private final File playlistsFile;
    private final File historyFile;
    private final File binaryBackupFile;

    public FileStorage(String dataDirectory) {
        this.dataDirectory = new File(dataDirectory);
        this.songsFile = new File(this.dataDirectory, "songs.txt");
        this.playlistsFile = new File(this.dataDirectory, "playlists.txt");
        this.historyFile = new File(this.dataDirectory, "history.txt");
        this.binaryBackupFile = new File(this.dataDirectory, "library_backup.bin");
    }

    public MusicLibrary load() throws IOException {
        if (!dataDirectory.exists()) {
            dataDirectory.mkdirs();
        }

        MusicLibrary library = new MusicLibrary();
        loadSongs(library);
        loadPlaylists(library);
        loadHistory(library);

        if (library.getSongs().size() == 0 && binaryBackupFile.exists()) {
            MusicLibrary backup = loadBinaryBackup();
            if (backup != null) {
                return backup;
            }
        }
        return library;
    }

    public void save(MusicLibrary library) throws IOException {
        if (!dataDirectory.exists()) {
            dataDirectory.mkdirs();
        }
        writeRecords(songsFile, library.getSongs());
        writeRecords(playlistsFile, library.getPlaylists());
        writeRecords(historyFile, library.getHistory());
        saveBinaryBackup(library);
    }

    public static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("|", "\\p").replace("\n", "\\n");
    }

    public static String unescape(String value) {
        StringBuilder result = new StringBuilder();
        boolean escaping = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (escaping) {
                if (c == 'p') {
                    result.append('|');
                } else if (c == 'n') {
                    result.append('\n');
                } else {
                    result.append(c);
                }
                escaping = false;
            } else if (c == '\\') {
                escaping = true;
            } else {
                result.append(c);
            }
        }
        if (escaping) {
            result.append('\\');
        }
        return result.toString();
    }

    private void loadSongs(MusicLibrary library) throws IOException {
        if (!songsFile.exists()) {
            return;
        }

        Scanner scanner = null;
        try {
            scanner = new Scanner(songsFile, "UTF-8");
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.trim().length() == 0) {
                    continue;
                }
                List<String> parts = splitRecord(line);
                if (parts.size() < 10) {
                    continue;
                }
                Song song = new Song(
                        unescape(parts.get(0)),
                        unescape(parts.get(1)),
                        unescape(parts.get(2)),
                        unescape(parts.get(3)),
                        unescape(parts.get(4)),
                        Integer.parseInt(parts.get(5)),
                        unescape(parts.get(6)),
                        Boolean.parseBoolean(parts.get(7)),
                        Integer.parseInt(parts.get(8)),
                        LocalDateTime.parse(parts.get(9), LibraryItem.TIME_FORMAT));
                try {
                    library.addSong(song);
                } catch (DuplicateItemException ignored) {
                    // Keep the first valid record when the text file has duplicates.
                }
            }
        } finally {
            if (scanner != null) {
                scanner.close();
            }
        }
    }

    private void loadPlaylists(MusicLibrary library) throws IOException {
        if (!playlistsFile.exists()) {
            return;
        }

        Scanner scanner = null;
        try {
            scanner = new Scanner(playlistsFile, "UTF-8");
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.trim().length() == 0) {
                    continue;
                }
                List<String> parts = splitRecord(line);
                if (parts.size() < 5) {
                    continue;
                }
                String id = unescape(parts.get(0));
                String name = unescape(parts.get(1));
                String songIdsText = unescape(parts.get(2));
                List<String> songIds = songIdsText.length() == 0
                        ? Collections.emptyList()
                        : Arrays.asList(songIdsText.split(","));
                LocalDateTime createdAt = LocalDateTime.parse(parts.get(3), LibraryItem.TIME_FORMAT);
                String type = parts.get(4);
                Playlist playlist;
                if ("FavoritePlaylist".equals(type)) {
                    playlist = new FavoritePlaylist(id, name, songIds, createdAt);
                } else {
                    playlist = new Playlist(id, name, songIds, createdAt);
                }
                try {
                    library.addPlaylist(playlist);
                } catch (DuplicateItemException ignored) {
                    // Keep the first valid record when the text file has duplicates.
                }
            }
        } finally {
            if (scanner != null) {
                scanner.close();
            }
        }
    }

    private void loadHistory(MusicLibrary library) throws IOException {
        if (!historyFile.exists()) {
            return;
        }

        Scanner scanner = null;
        try {
            scanner = new Scanner(historyFile, "UTF-8");
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.trim().length() == 0) {
                    continue;
                }
                List<String> parts = splitRecord(line);
                if (parts.size() < 5) {
                    continue;
                }
                library.getHistory().add(new PlaybackRecord(
                        unescape(parts.get(0)),
                        unescape(parts.get(1)),
                        unescape(parts.get(2)),
                        unescape(parts.get(3)),
                        LocalDateTime.parse(parts.get(4), LibraryItem.TIME_FORMAT)));
            }
        } finally {
            if (scanner != null) {
                scanner.close();
            }
        }
    }

    private void writeRecords(File file, List<? extends CsvConvertible> records) throws IOException {
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(file, "UTF-8");
            for (int i = 0; i < records.size(); i++) {
                writer.println(records.get(i).toCsv());
            }
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    private void saveBinaryBackup(MusicLibrary library) throws IOException {
        ObjectOutputStream out = null;
        FileOutputStream fileOut = null;
        try {
            fileOut = new FileOutputStream(binaryBackupFile);
            out = new ObjectOutputStream(fileOut);
            out.writeObject(library);
        } finally {
            if (out != null) {
                out.close();
            }
            if (fileOut != null) {
                fileOut.close();
            }
        }
    }

    private MusicLibrary loadBinaryBackup() {
        ObjectInputStream in = null;
        FileInputStream fileIn = null;
        try {
            fileIn = new FileInputStream(binaryBackupFile);
            in = new ObjectInputStream(fileIn);
            return (MusicLibrary) in.readObject();
        } catch (IOException e) {
            return null;
        } catch (ClassNotFoundException e) {
            return null;
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (fileIn != null) {
                    fileIn.close();
                }
            } catch (IOException ignored) {
                // Nothing else to do while closing a fallback backup file.
            }
        }
    }

    private static List<String> splitRecord(String line) {
        List<String> result = new ArrayList<String>();
        StringBuilder current = new StringBuilder();
        boolean escaping = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (escaping) {
                current.append('\\').append(c);
                escaping = false;
            } else if (c == '\\') {
                escaping = true;
            } else if (c == '|') {
                result.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        if (escaping) {
            current.append('\\');
        }
        result.add(current.toString());
        return result;
    }
}
