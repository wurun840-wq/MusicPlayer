package musicplayer.model;

import java.time.LocalDateTime;
import java.util.List;

public class FavoritePlaylist extends Playlist {
    public FavoritePlaylist(String id, String name) {
        super(id, name);
    }

    public FavoritePlaylist(String id, String name, List<String> songIds, LocalDateTime createdAt) {
        super(id, name, songIds, createdAt);
    }

    @Override
    public String getDisplayText() {
        return "[Favorites] " + super.getDisplayText();
    }

    @Override
    public String getType() {
        return "FavoritePlaylist";
    }
}
