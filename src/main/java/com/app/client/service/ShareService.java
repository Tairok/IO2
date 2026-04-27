package com.app.client.service;

import com.app.client.network.NetworkConnection;
import com.app.client.utils.AppLogger;
import com.app.client.patterns.observers.TransferAction;
import com.app.client.patterns.observers.TransferEvent;
import com.app.client.patterns.observers.TransferNotificationCenter;
import com.app.client.patterns.observers.TransferStage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * ShareService handles single file sharing with a separate connection.
 * 
 * Each share operation uses its own NetworkConnection to enable parallel
 * sharing of multiple files without blocking or conflicts.
 */
public class ShareService {
    private final DataInputStream dis;
    private final DataOutputStream dos;

    public ShareService(NetworkConnection connection) {
        this.dis = connection.in();
        this.dos = connection.out();
    }

    /**
     * Shares a single file from sender to recipient.
     * 
     * @param sender sender username
     * @param recipient recipient username
     * @param filename name of file to share
     * @return true if successful
     * @throws IOException on network/server error
     */
    public boolean shareFile(String sender, String recipient, String filename) throws IOException {
        TransferNotificationCenter.getInstance().notify(new TransferEvent(
                TransferAction.SHARE,
                TransferStage.STARTED,
                sender,
                recipient,
                filename,
                0,
                0,
                "Sharing started"
        ));

        try {
            dos.writeUTF("SHARE");
            dos.writeUTF(sender);
            dos.writeUTF(recipient);
            dos.writeUTF(filename);
            dos.flush();

            String response = dis.readUTF();
            if ("OK".equals(response)) {
                AppLogger.info("Share succeeded: " + sender + " -> " + recipient + " / " + filename);
                TransferNotificationCenter.getInstance().notify(new TransferEvent(
                        TransferAction.SHARE,
                        TransferStage.CONFIRMED,
                        sender,
                        recipient,
                        filename,
                        0,
                        0,
                        "Server confirmed share"
                ));
                return true;
            }

            AppLogger.warn("Share failed: " + response);
            throw new IOException(response);

        } catch (IOException e) {
            TransferNotificationCenter.getInstance().notify(new TransferEvent(
                    TransferAction.SHARE,
                    TransferStage.FAILED,
                    sender,
                    recipient,
                    filename,
                    0,
                    0,
                    e.getMessage()
            ));
            throw new IOException("Share failed: " + e.getMessage(), e);
        }
    }
}
