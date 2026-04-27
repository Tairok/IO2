package com.app.client.task;

import com.app.client.service.ShareService;
import javafx.concurrent.Task;

/**
 * ShareTask wraps a single file sharing operation into a JavaFX Task.
 * 
 * This allows share operations to run in background threads without blocking the UI.
 * Each file share gets its own NetworkConnection and ShareService to enable
 * true parallel sharing of multiple files.
 */
public class ShareTask extends Task<Boolean> {
    private final String sender;
    private final String recipient;
    private final String filename;
    private final ShareService shareService;

    public ShareTask(String sender, String recipient, String filename, ShareService shareService) {
        this.sender = sender;
        this.recipient = recipient;
        this.filename = filename;
        this.shareService = shareService;
    }

    @Override
    protected Boolean call() throws Exception {
        return shareService.shareFile(sender, recipient, filename);
    }
}
