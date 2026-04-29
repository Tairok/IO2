package com.app.client.service;

import com.app.client.network.NetworkConnection;
import com.app.client.utils.AppLogger;
import com.app.client.patterns.observers.TransferAction;
import com.app.client.patterns.observers.TransferEvent;
import com.app.client.patterns.observers.TransferNotificationCenter;
import com.app.client.patterns.observers.TransferStage;

import java.io.*;
import java.nio.file.Files;
import java.util.function.BiConsumer;

/**
 * TransferService handles bidirectional file transfers with progress tracking.
 * 
 * Key features:
 * - Each file gets its own connection (passed in constructor) for parallel transfers
 * - Progress callbacks are called frequently for real-time UI updates
 * - Both upload and download report (bytes_transferred, total_bytes)
 * - Efficient buffering for large files
 */
public class TransferService {
    private final DataOutputStream dos;
    private final DataInputStream  dis;

    public TransferService(NetworkConnection conn) {
        this.dos = conn.out();
        this.dis = conn.in();
    }

    /**
     * Uploads a single file, calling progress.accept(sent, total) as it goes.
     * Progress is updated every buffer chunk for smooth UI responsiveness.
     * Will throw IOException("Quota exceeded") if the plan quota forbids it.
     */
    public boolean upload(String user,
                          File file,
                          BiConsumer<Long,Long> progress) throws IOException {
        long total = file.length();
        long sent = 0;

        TransferNotificationCenter.getInstance().notify(new TransferEvent(
                TransferAction.UPLOAD,
                TransferStage.STARTED,
                user,
                null,
                file.getName(),
                0,
                total,
                "Upload started"
        ));

        try {
            dos.writeUTF("UPLOAD");
            dos.writeUTF(user);
            dos.writeUTF(file.getName());
            dos.writeLong(total);
            dos.flush();

            String reply = dis.readUTF();
            if (!"OK".equals(reply)) {
                if (reply.startsWith("ERR\tQUOTA_EXCEEDED")) {
                    throw new IOException("Quota exceeded");
                }
                throw new IOException("UPLOAD failed: " + reply);
            }

            AppLogger.info("Starting upload: " + file.getName() + " (" + total + " bytes)");

            try {
                progress.accept(0L, total);
            } catch (Exception ignored) {}

            try (InputStream fis = new FileInputStream(file)) {
                byte[] buf = new byte[8 * 1024];
                int read;
                while ((read = fis.read(buf)) != -1) {
                    dos.write(buf, 0, read);
                    sent += read;
                    try { progress.accept(sent, total); } catch (Exception ignored) {}
                }
                dos.flush();
            }

            String ack = dis.readUTF();
            if (!"OK".equals(ack)) {
                throw new IOException("UPLOAD incomplete: " + ack);
            }

            try { progress.accept(total, total); } catch (Exception ignored) {}

            AppLogger.info("Upload completed: " + file.getName());
            TransferNotificationCenter.getInstance().notify(new TransferEvent(
                    TransferAction.UPLOAD,
                    TransferStage.CONFIRMED,
                    user,
                    null,
                    file.getName(),
                    total,
                    total,
                    "Server confirmed upload"
            ));
            return true;

        } catch (IOException e) {
            TransferNotificationCenter.getInstance().notify(new TransferEvent(
                    TransferAction.UPLOAD,
                    TransferStage.FAILED,
                    user,
                    null,
                    file.getName(),
                    sent,
                    total,
                    e.getMessage()
            ));
            throw e;
        }
    }

    /**
     * Downloads into destFile, updating progress.accept(received, total).
     * Progress is updated every buffer chunk for smooth UI responsiveness.
     * Returns true on success.
     */
    public boolean download(String user,
                            String name,
                            File destFile,
                            BiConsumer<Long,Long> progress) throws IOException {
        long recv = 0;

        TransferNotificationCenter.getInstance().notify(new TransferEvent(
                TransferAction.DOWNLOAD,
                TransferStage.STARTED,
                user,
                null,
                name,
                0,
                -1,
                "Download started"
        ));

        try {
            dos.writeUTF("DOWNLOAD");
            dos.writeUTF(user);
            dos.writeUTF(name);
            dos.flush();

            long total = dis.readLong();
            if (total < 0) {
                AppLogger.warn("Download failed: file not found - " + name);
                TransferNotificationCenter.getInstance().notify(new TransferEvent(
                        TransferAction.DOWNLOAD,
                        TransferStage.FAILED,
                        user,
                        null,
                        name,
                        0,
                        0,
                        "FILE_NOT_FOUND"
                ));
                return false;
            }

            AppLogger.info("Starting download: " + name + " (" + total + " bytes)");

            destFile.getParentFile().mkdirs();
            try (OutputStream fos = Files.newOutputStream(destFile.toPath())) {
                byte[] buf = new byte[8 * 1024];
                int r;
                while (recv < total && (r = dis.read(buf,0,(int)Math.min(buf.length, total-recv))) > 0) {
                    fos.write(buf,0,r);
                    recv += r;
                    progress.accept(recv, total);
                }
            }

            progress.accept(total, total);
            AppLogger.info("Download completed: " + name);
            TransferNotificationCenter.getInstance().notify(new TransferEvent(
                    TransferAction.DOWNLOAD,
                    TransferStage.CONFIRMED,
                    user,
                    null,
                    name,
                    total,
                    total,
                    "Download saved locally"
            ));
            return true;

        } catch (IOException e) {
            TransferNotificationCenter.getInstance().notify(new TransferEvent(
                    TransferAction.DOWNLOAD,
                    TransferStage.FAILED,
                    user,
                    null,
                    name,
                    recv,
                    0,
                    e.getMessage()
            ));
            throw e;
        }
    }
}
