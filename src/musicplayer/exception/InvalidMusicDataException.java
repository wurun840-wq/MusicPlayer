package musicplayer.exception;

public class InvalidMusicDataException extends Exception {
    public InvalidMusicDataException(String message) {
        super(message);
    }

    public InvalidMusicDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
