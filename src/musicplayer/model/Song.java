package musicplayer.model;

import musicplayer.exception.InvalidMusicDataException;
import musicplayer.service.PlayerService;
import musicplayer.storage.FileStorage;

import java.time.LocalDateTime;
import java.util.Objects;

public class Song extends LibraryItem implements Playable, CsvConvertible {
    private String title;
    private String artist;
    private String album;
    private String genre;
    private int durationSeconds;
    private String filePath;
    private boolean favorite;
    private int playCount;

    public Song(String id, String title, String artist, String album, String genre,
                int durationSeconds, String filePath) {
        super(id, title);
        setDetails(title, artist, album, genre, durationSeconds, filePath);
    }

    public Song(String id, String title, String artist, String album, String genre,
                int durationSeconds, String filePath, boolean favorite, int playCount,
                LocalDateTime createdAt) {
        super(id, title, createdAt);
        setDetails(title, artist, album, genre, durationSeconds, filePath);
        this.favorite = favorite;
        this.playCount = playCount;
    }

    public void setDetails(String title, String artist, String album, String genre,
                           int durationSeconds, String filePath) {
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.genre = genre;
        this.durationSeconds = durationSeconds;
        this.filePath = filePath;
        setName(title);
    }

    public void updatePlayCount() {
        playCount++;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getAlbum() {
        return album;
    }

    public String getGenre() {
        return genre;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public String getFilePath() {
        return filePath;
    }

    public boolean isFavorite() {
        return favorite;
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    public int getPlayCount() {
        return playCount;
    }

    public String getDurationText() {
        int minutes = durationSeconds / 60;
        int seconds = durationSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
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
        return title + " - " + artist;
    }

    @Override
    public String getType() {
        return "Song";
    }

    @Override
    public String toCsv() {
        return String.join("|",
                FileStorage.escape(getId()),
                FileStorage.escape(title),
                FileStorage.escape(artist),
                FileStorage.escape(album),
                FileStorage.escape(genre),
                String.valueOf(durationSeconds),
                FileStorage.escape(filePath),
                String.valueOf(favorite),
                String.valueOf(playCount),
                getCreatedAt().format(TIME_FORMAT));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Song)) {
            return false;
        }
        Song other = (Song) obj;
        return Objects.equals(getId(), other.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }
}
