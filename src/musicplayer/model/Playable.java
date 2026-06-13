package musicplayer.model;

import musicplayer.exception.InvalidMusicDataException;
import musicplayer.service.PlayerService;

public interface Playable {
    void play(PlayerService player) throws InvalidMusicDataException;

    void stop(PlayerService player);
}
