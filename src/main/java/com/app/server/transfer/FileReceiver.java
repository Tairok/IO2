package com.app.server.transfer;

import com.app.server.utils.AppLogger;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileReceiver {
    private static final int BUFFER_SIZE = 4096;

    public static void receive(DataInputStream dis, String baseDir) throws IOException {
        String username = dis.readUTF();
        String fileName = dis.readUTF();
        long fileSize = dis.readLong();

        AppLogger.info("Receiving file: " + fileName + " from user: " + username);

        File userDir = new File(baseDir, username);
        if (!userDir.exists() && !userDir.mkdirs()) {
            throw new IOException("Failed to create directory: " + userDir);
        }

        File outputFile = new File(userDir, fileName);
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            long totalRead = 0;
            int read;
            while (totalRead < fileSize && (read = dis.read(buffer, 0,
                    (int)Math.min(buffer.length, fileSize - totalRead))) != -1) {
                fos.write(buffer, 0, read);
                totalRead += read;
            }
            fos.flush();
        }

        AppLogger.info("File saved: " + outputFile.getAbsolutePath());
    }
}
