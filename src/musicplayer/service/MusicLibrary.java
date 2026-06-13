package musicplayer.service;

import musicplayer.exception.DuplicateItemException;
import musicplayer.exception.InvalidMusicDataException;
import musicplayer.model.FavoritePlaylist;
import musicplayer.model.PlaybackRecord;
import musicplayer.model.Playlist;
import musicplayer.model.Song;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class MusicLibrary implements Serializable {
    private final List<Song> songs = new ArrayList<Song>();
    private final List<Playlist> playlists = new ArrayList<Playlist>();
    private final List<PlaybackRecord> history = new ArrayList<PlaybackRecord>();
    private int nextIdNumber = 1;

    public List<Song> getSongs() {
        return songs;
    }

    public List<Playlist> getPlaylists() {
        return playlists;
    }

    public List<PlaybackRecord> getHistory() {
        return history;
    }

    public Song addSong(String title, String artist, String album, String genre,
                        int durationSeconds, String filePath)
            throws DuplicateItemException, InvalidMusicDataException {
        validateSong(title, artist, durationSeconds);
        for (int i = 0; i < songs.size(); i++) {
            Song song = songs.get(i);
            if (song.getTitle().equalsIgnoreCase(title)
                    && song.getArtist().equalsIgnoreCase(artist)) {
                throw new DuplicateItemException("A song with the same title and artist already exists.");
            }
        }
        Song song = new Song(newId(), title, artist, album, genre, durationSeconds, filePath);
        songs.add(song);
        return song;
    }

    public void addSong(Song song) throws DuplicateItemException {
        if (findSongById(song.getId()) != null) {
            throw new DuplicateItemException("Song id already exists.");
        }
        songs.add(song);
    }

    public void updateSong(String songId, String title, String artist, String album, String genre,
                           int durationSeconds, String filePath)
            throws InvalidMusicDataException {
        validateSong(title, artist, durationSeconds);
        Song song = getSongOrThrow(songId);
        song.setDetails(title, artist, album, genre, durationSeconds, filePath);
    }

    public void deleteSong(String songId) {
        for (int i = songs.size() - 1; i >= 0; i--) {
            if (songs.get(i).getId().equals(songId)) {
                songs.remove(i);
            }
        }
        for (int i = 0; i < playlists.size(); i++) {
            playlists.get(i).removeSong(songId);
        }
    }

    public Playlist createPlaylist(String name) throws DuplicateItemException, InvalidMusicDataException {
        return createPlaylist(name, false);
    }

    public Playlist createPlaylist(String name, boolean favoriteType)
            throws DuplicateItemException, InvalidMusicDataException {
        validateName(name, "Playlist name");
        for (int i = 0; i < playlists.size(); i++) {
            if (playlists.get(i).getName().equalsIgnoreCase(name)) {
                throw new DuplicateItemException("A playlist with this name already exists.");
            }
        }

        Playlist playlist;
        if (favoriteType) {
            playlist = new FavoritePlaylist(newId(), name);
        } else {
            playlist = new Playlist(newId(), name);
        }
        playlists.add(playlist);
        return playlist;
    }

    public void addPlaylist(Playlist playlist) throws DuplicateItemException {
        if (findPlaylistById(playlist.getId()) != null) {
            throw new DuplicateItemException("Playlist id already exists.");
        }
        playlists.add(playlist);
    }

    public void renamePlaylist(String playlistId, String newName) throws InvalidMusicDataException {
        validateName(newName, "Playlist name");
        getPlaylistOrThrow(playlistId).setName(newName);
    }

    public void deletePlaylist(String playlistId) {
        for (int i = playlists.size() - 1; i >= 0; i--) {
            if (playlists.get(i).getId().equals(playlistId)) {
                playlists.remove(i);
            }
        }
    }

    public void addSongToPlaylist(String playlistId, String songId)
            throws DuplicateItemException, InvalidMusicDataException {
        Playlist playlist = getPlaylistOrThrow(playlistId);
        Song song = getSongOrThrow(songId);
        playlist.addSong(song);
    }

    public void removeSongFromPlaylist(String playlistId, String songId) throws InvalidMusicDataException {
        getPlaylistOrThrow(playlistId).removeSong(songId);
    }

    public void toggleFavorite(String songId) throws InvalidMusicDataException {
        Song song = getSongOrThrow(songId);
        song.setFavorite(!song.isFavorite());
    }

    public void recordPlayback(Song song) {
        song.updatePlayCount();
        history.add(0, new PlaybackRecord(newId(), song));
        if (history.size() > 200) {
            history.remove(history.size() - 1);
        }
    }

    public Song findSongById(String id) {
        for (int i = 0; i < songs.size(); i++) {
            Song song = songs.get(i);
            if (song.getId().equals(id)) {
                return song;
            }
        }
        return null;
    }

    public Playlist findPlaylistById(String id) {
        for (int i = 0; i < playlists.size(); i++) {
            Playlist playlist = playlists.get(i);
            if (playlist.getId().equals(id)) {
                return playlist;
            }
        }
        return null;
    }

    public Song getSongOrThrow(String id) throws InvalidMusicDataException {
        Song song = findSongById(id);
        if (song == null) {
            throw new InvalidMusicDataException("Song not found.");
        }
        return song;
    }

    public Playlist getPlaylistOrThrow(String id) throws InvalidMusicDataException {
        Playlist playlist = findPlaylistById(id);
        if (playlist == null) {
            throw new InvalidMusicDataException("Playlist not found.");
        }
        return playlist;
    }

    public List<Song> searchSongs(String keyword, String genre) {
        String query = "";
        if (keyword != null) {
            query = keyword.trim().toLowerCase();
        }
        String selectedGenre = "All";
        if (genre != null) {
            selectedGenre = genre;
        }

        List<Song> result = new ArrayList<Song>();
        for (int i = 0; i < songs.size(); i++) {
            Song song = songs.get(i);
            boolean genreMatches = selectedGenre.equals("All")
                    || song.getGenre().equalsIgnoreCase(selectedGenre);
            boolean textMatches = query.length() == 0
                    || song.getTitle().toLowerCase().contains(query)
                    || song.getArtist().toLowerCase().contains(query)
                    || song.getAlbum().toLowerCase().contains(query);
            if (genreMatches && textMatches) {
                result.add(song);
            }
        }
        return result;
    }

    public List<Song> sortSongsByTitle() {
        List<Song> result = copySongs();
        for (int i = 0; i < result.size() - 1; i++) {
            for (int j = i + 1; j < result.size(); j++) {
                if (result.get(i).getTitle().compareToIgnoreCase(result.get(j).getTitle()) > 0) {
                    Song temp = result.get(i);
                    result.set(i, result.get(j));
                    result.set(j, temp);
                }
            }
        }
        return result;
    }

    public List<Song> sortSongsByArtist() {
        List<Song> result = copySongs();
        for (int i = 0; i < result.size() - 1; i++) {
            for (int j = i + 1; j < result.size(); j++) {
                if (result.get(i).getArtist().compareToIgnoreCase(result.get(j).getArtist()) > 0) {
                    Song temp = result.get(i);
                    result.set(i, result.get(j));
                    result.set(j, temp);
                }
            }
        }
        return result;
    }

    public List<Song> getSongsInPlaylist(Playlist playlist) {
        List<Song> result = new ArrayList<Song>();
        List<String> ids = playlist.getSongIds();
        for (int i = 0; i < ids.size(); i++) {
            Song song = findSongById(ids.get(i));
            if (song != null) {
                result.add(song);
            }
        }
        return result;
    }

    public List<String> getGenres() {
        List<String> genres = new ArrayList<String>();
        genres.add("All");
        for (int i = 0; i < songs.size(); i++) {
            String genre = songs.get(i).getGenre();
            if (genre != null && genre.trim().length() > 0 && !containsIgnoreCase(genres, genre)) {
                genres.add(genre);
            }
        }
        for (int i = 1; i < genres.size() - 1; i++) {
            for (int j = i + 1; j < genres.size(); j++) {
                if (genres.get(i).compareToIgnoreCase(genres.get(j)) > 0) {
                    String temp = genres.get(i);
                    genres.set(i, genres.get(j));
                    genres.set(j, temp);
                }
            }
        }
        return genres;
    }

    public String getStatisticsReport() {
        int totalDuration = 0;
        int favorites = 0;
        Song topSong = null;

        List<String> genreNames = new ArrayList<String>();
        List<Integer> genreCounts = new ArrayList<Integer>();

        for (int i = 0; i < songs.size(); i++) {
            Song song = songs.get(i);
            totalDuration = totalDuration + song.getDurationSeconds();
            if (song.isFavorite()) {
                favorites++;
            }
            if (topSong == null || song.getPlayCount() > topSong.getPlayCount()) {
                topSong = song;
            }
            addGenreCount(genreNames, genreCounts, song.getGenre());
        }

        String genreSummary = buildGenreSummary(genreNames, genreCounts);

        return "Library songs: " + songs.size()
                + "\nPlaylists: " + playlists.size()
                + "\nTotal listening length: " + formatDuration(totalDuration)
                + "\nFavorite songs: " + favorites
                + "\nPlayback records: " + history.size()
                + "\nMost played: " + (topSong == null ? "N/A" : topSong.getDisplayText()
                + " (" + topSong.getPlayCount() + " plays)")
                + "\nGenres: " + (genreSummary.length() == 0 ? "N/A" : genreSummary);
    }

    public static String formatDuration(int seconds) {
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        int remaining = seconds % 60;
        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, remaining);
        }
        return String.format("%d:%02d", minutes, remaining);
    }

    private List<Song> copySongs() {
        List<Song> result = new ArrayList<Song>();
        for (int i = 0; i < songs.size(); i++) {
            result.add(songs.get(i));
        }
        return result;
    }

    private boolean containsIgnoreCase(List<String> values, String target) {
        for (int i = 0; i < values.size(); i++) {
            if (values.get(i).equalsIgnoreCase(target)) {
                return true;
            }
        }
        return false;
    }

    private void addGenreCount(List<String> genreNames, List<Integer> genreCounts, String genre) {
        for (int i = 0; i < genreNames.size(); i++) {
            if (genreNames.get(i).equalsIgnoreCase(genre)) {
                genreCounts.set(i, genreCounts.get(i) + 1);
                return;
            }
        }
        genreNames.add(genre);
        genreCounts.add(1);
    }

    private String buildGenreSummary(List<String> genreNames, List<Integer> genreCounts) {
        for (int i = 0; i < genreNames.size() - 1; i++) {
            for (int j = i + 1; j < genreNames.size(); j++) {
                if (genreNames.get(i).compareToIgnoreCase(genreNames.get(j)) > 0) {
                    String tempName = genreNames.get(i);
                    genreNames.set(i, genreNames.get(j));
                    genreNames.set(j, tempName);

                    Integer tempCount = genreCounts.get(i);
                    genreCounts.set(i, genreCounts.get(j));
                    genreCounts.set(j, tempCount);
                }
            }
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < genreNames.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(genreNames.get(i)).append(": ").append(genreCounts.get(i));
        }
        return builder.toString();
    }

    private void validateSong(String title, String artist, int durationSeconds)
            throws InvalidMusicDataException {
        validateName(title, "Title");
        validateName(artist, "Artist");
        if (durationSeconds <= 0) {
            throw new InvalidMusicDataException("Duration must be greater than 0 seconds.");
        }
    }

    private void validateName(String value, String fieldName) throws InvalidMusicDataException {
        if (value == null || value.trim().isEmpty()) {
            throw new InvalidMusicDataException(fieldName + " cannot be empty.");
        }
    }

    private String newId() {
        String id = "ID" + System.currentTimeMillis() + "_" + nextIdNumber;
        nextIdNumber++;
        return id;
    }
}
