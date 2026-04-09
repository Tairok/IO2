// com/capp/client/service/TransferService.java
package com.app.client.service;

import com.app.client.network.NetworkConnection;

import java.io.*;
import java.nio.file.Files;
import java.util.function.BiConsumer;

public class TransferService {
    private final DataOutputStream dos;
    private final DataInputStream  dis;

    public TransferService(NetworkConnection conn) {
        this.dos = conn.out();
        this.dis = conn.in();
    }

    /**
     * Uploads a single file, calling progress.accept(sent, total) as it goes.
     * Will throw IOException("Quota exceeded") if the plan quota forbids it.
     */
    public boolean upload(String user,
                          File file,
                          BiConsumer<Long,Long> progress) throws IOException {
        long total = file.length();

        // 1) handshake + quota check
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

        // 2) stream the bytes
        try (InputStream fis = new FileInputStream(file)) {
            byte[] buf = new byte[4096];
            long sent = 0;
            int read;
            while ((read = fis.read(buf)) != -1) {
                dos.write(buf, 0, read);
                sent += read;
                progress.accept(sent, total);
            }
            dos.flush();
        }

        // 3) final ACK
        String ack = dis.readUTF();
        if (!"OK".equals(ack)) {
            throw new IOException("UPLOAD incomplete: " + ack);
        }
        return true;
    }

    /**
     * Downloads into destFile, updating progress.accept(received, total).
     * Returns true on success.
     */
    public boolean download(String user,
                            String name,
                            File destFile,
                            BiConsumer<Long,Long> progress) throws IOException {
        // 1) metadata request
        dos.writeUTF("DOWNLOAD");
        dos.writeUTF(user);
        dos.writeUTF(name);
        dos.flush();

        long total = dis.readLong();
        if (total < 0) return false;

        destFile.getParentFile().mkdirs();
        try (OutputStream fos = Files.newOutputStream(destFile.toPath())) {
            byte[] buf = new byte[4096];
            long recv = 0;
            int r;
            while (recv < total && (r = dis.read(buf,0,(int)Math.min(buf.length, total-recv))) > 0) {
                fos.write(buf,0,r);
                recv += r;
                progress.accept(recv, total);
            }
        }
        progress.accept(total, total);
        return true;
    }
}
