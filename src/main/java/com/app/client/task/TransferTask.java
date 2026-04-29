package com.app.client.task;

import com.app.client.service.FileService;
import javafx.concurrent.Task;

import java.io.File;
import java.util.function.BiConsumer;

/**
 * TransferTask runs file upload/download in a background thread using JavaFX Task.
 * This ensures the UI remains responsive during long file transfers.
 * Progress updates are sent to the UI thread via Task's progressProperty.
 * 
 * Each file gets its own NetworkConnection and TransferService to enable
 * true parallel transfers without blocking or data conflicts.
 */
public class TransferTask extends Task<Boolean> {
    private final String username;
    private final File file;
    private final FileService fileService;
    private final BiConsumer<Long, Long> progressListener;

    public TransferTask(String username, File file, FileService fileService) {
        this.username    = username;
        this.file        = file;
        this.fileService = fileService;
        this.progressListener = null;
    }

    public TransferTask(String username,
                        File file,
                        FileService fileService,
                        BiConsumer<Long, Long> progressListener) {
        this.username = username;
        this.file = file;
        this.fileService = fileService;
        this.progressListener = progressListener;
    }

    /**
     * Executes a single upload and reports progress through the JavaFX {@link Task} API.
     */
    @Override
    protected Boolean call() throws Exception {
        return fileService.uploadFile(username, file, (sent, total) -> {
            updateProgress(sent, total);
            if (progressListener != null) {
                progressListener.accept(sent, total);
            }
        });
    }
}
