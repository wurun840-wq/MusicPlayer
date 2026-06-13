package musicplayer.model;

import musicplayer.exception.DuplicateItemException;
import musicplayer.exception.InvalidMusicDataException;
import musicplayer.service.PlayerService;
import musicplayer.storage.FileStorage;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Playlist extends LibraryItem implements Playable, CsvConvertible {
    private final List<String> songIds = new ArrayList<String>();

    public Playlist(String id, String name) {
        super(id, name);
    }

    public Playlist(String id, String name, List<String> songIds, LocalDateTime createdAt) {
        super(id, name, createdAt);
        this.songIds.addAll(songIds);
    }

    public void addSong(Song song) throws DuplicateItemException {
        addSong(song.getId());
    }

    public void addSong(String songId) throws DuplicateItemException {
        if (songIds.contains(songId)) {
            throw new DuplicateItemException("This song already exists in the playlist.");
        }
        songIds.add(songId);
    }

    public void removeSong(String songId) {
        songIds.remove(songId);
    }

    public List<String> getSongIds() {
        return Collections.unmodifiableList(songIds);
    }

    public int getSongCount() {
        return songIds.size();
    }

    @Override
    public void play(PlayerService player) throws InvalidMusicDataException {
        player.play(this);
    }

    @Override
    public void stop(PlayerService player) {
        player.stop();
    }

    @Override
    public String getDisplayText() {
        return getName() + " (" + songIds.size() + " songs)";
    }

    @Override
    public String getType() {
        return "Playlist";
    }

    @Override
    public String toCsv() {
        return String.join("|",
                FileStorage.escape(getId()),
                FileStorage.escape(getName()),
                FileStorage.escape(String.join(",", songIds)),
                getCreatedAt().format(TIME_FORMAT),
                getType());
    }
}
