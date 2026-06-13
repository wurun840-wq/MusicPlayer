package musicplayer.service;

import musicplayer.exception.InvalidMusicDataException;
import musicplayer.model.Playlist;
import musicplayer.model.Song;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;

public class PlayerService {
    private final MusicLibrary library;
    private final Random random = new Random();
    private Clip clip;
    private Song currentSong;
    private Playlist currentPlaylist;
    private int playlistIndex;
    private boolean shuffle;
    private boolean repeat;
    private boolean paused;

    public PlayerService(MusicLibrary library) {
        this.library = library;
    }

    public void play(Song song) throws InvalidMusicDataException {
        closeClip();
        currentSong = song;
        currentPlaylist = null;
        paused = false;
        startSong(song);
    }

    public void play(Playlist playlist) throws InvalidMusicDataException {
        List<Song> songs = library.getSongsInPlaylist(playlist);
        if (songs.isEmpty()) {
            throw new InvalidMusicDataException("The selected playlist has no playable songs.");
        }
        currentPlaylist = playlist;
        playlistIndex = shuffle ? random.nextInt(songs.size()) : 0;
        startSong(songs.get(playlistIndex));
    }

    public void pause() {
        if (clip != null && clip.isRunning()) {
            clip.stop();
            paused = true;
        }
    }

    public void resume() {
        if (clip != null && paused) {
            clip.start();
            paused = false;
        }
    }

    public void stop() {
        closeClip();
        paused = false;
    }

    public void next() throws InvalidMusicDataException {
        if (currentPlaylist == null) {
            playNextLibrarySong();
            return;
        }
        List<Song> songs = library.getSongsInPlaylist(currentPlaylist);
        if (songs.isEmpty()) {
            throw new InvalidMusicDataException("Current playlist is empty.");
        }
        if (shuffle) {
            playlistIndex = getRandomNextIndex(songs.size(), playlistIndex);
        } else {
            playlistIndex = playlistIndex + 1;
        }
        if (playlistIndex >= songs.size()) {
            playlistIndex = 0;
        }
        startSong(songs.get(playlistIndex));
    }

    public void toggleShuffle() {
        shuffle = !shuffle;
    }

    public void toggleRepeat() {
        repeat = !repeat;
    }

    public boolean isShuffle() {
        return shuffle;
    }

    public boolean isRepeat() {
        return repeat;
    }

    public Song getCurrentSong() {
        return currentSong;
    }

    private void playNextLibrarySong() throws InvalidMusicDataException {
        List<Song> songs = library.getSongs();
        if (songs.isEmpty()) {
            throw new InvalidMusicDataException("The music library is empty.");
        }

        int currentIndex = findCurrentSongIndex(songs);
        int nextIndex;
        if (shuffle) {
            nextIndex = getRandomNextIndex(songs.size(), currentIndex);
        } else {
            nextIndex = currentIndex + 1;
        }

        if (currentIndex < 0) {
            nextIndex = 0;
        }
        if (nextIndex >= songs.size()) {
            nextIndex = 0;
        }
        startSong(songs.get(nextIndex));
    }

    private int findCurrentSongIndex(List<Song> songs) {
        if (currentSong == null) {
            return -1;
        }
        for (int i = 0; i < songs.size(); i++) {
            if (songs.get(i).getId().equals(currentSong.getId())) {
                return i;
            }
        }
        return -1;
    }

    private int getRandomNextIndex(int size, int currentIndex) {
        if (size <= 1) {
            return 0;
        }
        int nextIndex = random.nextInt(size);
        while (nextIndex == currentIndex) {
            nextIndex = random.nextInt(size);
        }
        return nextIndex;
    }

    private void startSong(Song song) throws InvalidMusicDataException {
        closeClip();
        currentSong = song;
        library.recordPlayback(song);
        String path = song.getFilePath();
        if (path == null || path.trim().isEmpty()) {
            return;
        }
        File file = new File(path);
        if (!file.exists()) {
            return;
        }
        if (!isJavaSoundFile(path)) {
            return;
        }
        try (AudioInputStream stream = AudioSystem.getAudioInputStream(file)) {
            clip = AudioSystem.getClip();
            clip.open(stream);
            clip.start();
        } catch (UnsupportedAudioFileException | IOException | RuntimeException e) {
            throw new InvalidMusicDataException("Cannot play this audio file. Java Sound supports WAV, AIFF, and AU best.", e);
        } catch (Exception e) {
            throw new InvalidMusicDataException("Audio device is not available.", e);
        }
    }

    private boolean isJavaSoundFile(String path) {
        String lower = path.toLowerCase();
        return lower.endsWith(".wav") || lower.endsWith(".aiff") || lower.endsWith(".aif") || lower.endsWith(".au");
    }

    private void closeClip() {
        if (clip != null) {
            clip.stop();
            clip.close();
            clip = null;
        }
    }
}
