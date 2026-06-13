package musicplayer.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public abstract class LibraryItem implements Serializable {
    public static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final String id;
    private String name;
    private final LocalDateTime createdAt;

    protected LibraryItem(String id, String name) {
        this(id, name, LocalDateTime.now());
    }

    protected LibraryItem(String id, String name, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public abstract String getDisplayText();

    public abstract String getType();

    @Override
    public String toString() {
        return getDisplayText();
    }
}
