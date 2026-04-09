// com/capp/client/task/TransferTask.java
package com.app.client.task;

import com.app.client.service.FileService;
import javafx.concurrent.Task;

import java.io.File;

/**
 * Wraps FileService.uploadFile into a JavaFX Task<Boolean>.
 *
 * This class is used for file upload/download operations and runs in a background thread.
 * It ensures that file transfers do not block the JavaFX UI thread.
 */
/**
 * TransferTask runs file upload/download in a background thread using JavaFX Task.
 * This ensures the UI remains responsive during long file transfers.
 * Progress updates are sent to the UI thread via Task's progressProperty.
 */
public class TransferTask extends Task<Boolean> {
    private final String username;
    private final File file;
    private final FileService fileService;

    public TransferTask(String username, File file, FileService fileService) {
        this.username    = username;
        this.file        = file;
        this.fileService = fileService;
    }

    @Override
    protected Boolean call() throws Exception {
        return fileService.uploadFile(username, file, this::updateProgress);
    }
}
