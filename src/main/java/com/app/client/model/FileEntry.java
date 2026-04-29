package com.app.client.model;

import javafx.beans.property.*;

/**
 * UI-friendly representation of a remote file entry.
 */
public class FileEntry {
    private final StringProperty filename;
    private final LongProperty size;
    private final StringProperty lastModified;

    public FileEntry(String filename, long size, String lastModified) {
        this.filename = new SimpleStringProperty(filename);
        this.size = new SimpleLongProperty(size);
        this.lastModified = new SimpleStringProperty(lastModified);
    }

    public StringProperty filenameProperty() {
        return filename;
    }

    public LongProperty sizeProperty() {
        return size;
    }

    public StringProperty lastModifiedProperty() {
        return lastModified;
    }

    /**
     * Returns the filename value.
     *
     * @return file name
     */
    public String getFilename() {
        return filename.get();
    }
}
