package musicplayer.model;

import musicplayer.storage.FileStorage;

import java.time.LocalDateTime;

public class PlaybackRecord extends LibraryItem implements CsvConvertible {
    private final String songId;
    private final String songTitle;
    private final String artist;
    private final LocalDateTime playedAt;

    public PlaybackRecord(String id, Song song) {
        this(id, song.getId(), song.getTitle(), song.getArtist(), LocalDateTime.now());
    }

    public PlaybackRecord(String id, String songId, String songTitle, String artist, LocalDateTime playedAt) {
        super(id, songTitle, playedAt);
        this.songId = songId;
        this.songTitle = songTitle;
        this.artist = artist;
        this.playedAt = playedAt;
    }

    public String getSongId() {
        return songId;
    }

    public String getSongTitle() {
        return songTitle;
    }

    public String getArtist() {
        return artist;
    }

    public LocalDateTime getPlayedAt() {
        return playedAt;
    }

    @Override
    public String getDisplayText() {
        return songTitle + " - " + artist + " at " + playedAt;
    }

    @Override
    public String getType() {
        return "PlaybackRecord";
    }

    @Override
    public String toCsv() {
        return String.join("|",
                FileStorage.escape(getId()),
                FileStorage.escape(songId),
                FileStorage.escape(songTitle),
                FileStorage.escape(artist),
                playedAt.format(TIME_FORMAT));
    }
}
